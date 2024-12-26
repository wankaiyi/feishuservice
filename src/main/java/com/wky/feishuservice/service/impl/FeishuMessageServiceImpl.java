package com.wky.feishuservice.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.constants.FeishuConstants;
import com.wky.feishuservice.enumurations.FeishuCallbackActionTag;
import com.wky.feishuservice.enumurations.ReceiveType;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.mapper.PromptMapper;
import com.wky.feishuservice.mapper.UserPromptMapper;
import com.wky.feishuservice.mapper.UserPromptSubmissionsMapper;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.model.dto.FeishuCallbackRequestDTO;
import com.wky.feishuservice.model.dto.FeishuCallbackResponseDTO;
import com.wky.feishuservice.model.dto.FeishuP2pChatDTO;
import com.wky.feishuservice.model.po.PromptDO;
import com.wky.feishuservice.model.po.UserPromptDO;
import com.wky.feishuservice.model.po.UserPromptSubmissionsDO;
import com.wky.feishuservice.service.FeishuMessageService;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategyFactory;
import com.wky.feishuservice.utils.RedisUtils;
import com.wky.feishuservice.utils.SpringContextUtil;
import com.wky.feishuservice.utils.UserInfoContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author wky
 * @date 2024/11/11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeishuMessageServiceImpl implements FeishuMessageService {

    private final RedisUtils redisUtils;
    private final UserPromptMapper userPromptMapper;
    private final UserPromptSubmissionsMapper userPromptSubmissionsMapper;
    private final RedissonClient redissonClient;
    private final PromptMapper promptMapper;
    private final FeishuClient feishuClient;

    @Override
    public JSONObject processFeishuNotice(FeishuP2pChatDTO feishuP2pChatDTO) {
        JSONObject res = null;
        if (StringUtils.equals("url_verification", feishuP2pChatDTO.getType())) {
            res = new JSONObject();
            res.put("challenge", feishuP2pChatDTO.getChallenge());
            return res;
        }
        //处理事件消息
        handleMessage(feishuP2pChatDTO);
        return res;
    }

    @Override
    public FeishuCallbackResponseDTO processFeishuCallback(FeishuCallbackRequestDTO feishuCallbackRequestDTO) {
        FeishuCallbackResponseDTO response = new FeishuCallbackResponseDTO();
        if (StringUtils.equals("url_verification", feishuCallbackRequestDTO.getType())) {
            response.setChallenge(feishuCallbackRequestDTO.getChallenge());
            return response;
        }
        // 处理回调消息
        handleCallback(feishuCallbackRequestDTO, response);
        return response;
    }

    private void handleCallback(FeishuCallbackRequestDTO feishuCallbackRequestDTO, FeishuCallbackResponseDTO response) {
        FeishuCallbackRequestDTO.Header header = feishuCallbackRequestDTO.getHeader();
        long createTime = Long.parseLong(header.getCreateTime());
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime createTimeLDT = Instant.ofEpochMilli(createTime)
                .atZone(zoneId)
                .toLocalDateTime();
        if (createTimeLDT.isBefore(LocalDateTime.now().minusMinutes(5))) {
            // 超过5分钟的消息不处理
            return;
        }
        String eventId = header.getEventId();
        String eventKey = FeishuConstants.getFeishuEventKey(eventId);
        if (redisUtils.increment(eventKey) != 1) {
            log.info("事件重复，不处理，eventId: {}", eventId);
            return;
        } else {
            redisUtils.expire(eventKey, 60 * 60 * 1000, TimeUnit.MILLISECONDS);
        }
        String actionTag = feishuCallbackRequestDTO.getEvent().getAction().getTag();
        String openId = feishuCallbackRequestDTO.getEvent().getOperator().getOpenId();
        String openMessageId = feishuCallbackRequestDTO.getEvent().getContext().getOpenMessageId();

        if (StringUtils.equals(FeishuCallbackActionTag.SELECT_STATIC.getValue(), actionTag)) {
            handleSelectStatic(openId,
                    feishuCallbackRequestDTO.getEvent().getAction().getOption(),
                    openMessageId);
        } else if (StringUtils.equals(FeishuCallbackActionTag.BUTTON.getValue(), actionTag)) {
            RLock lock = redissonClient.getLock(FeishuConstants.getFeishuPromptSubmitLockKey(openId));
            try {
                if (lock.tryLock()) {
                    FeishuMessageServiceImpl self = SpringContextUtil.getBean(this.getClass());
                    self.handleClickButton(openId, openMessageId, response, feishuCallbackRequestDTO.getEvent().getToken());
                } else {
                    response.setToast(new FeishuCallbackResponseDTO.Toast()
                            .setType(FeishuCallbackResponseDTO.Toast.ToastType.WARNING.getValue())
                            .setContent("请求处理中，请稍后再试"));
                }
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } else {
            log.info("未知事件，eventId: {}, actionTag: {}", eventId, actionTag);
            response.setToast(new FeishuCallbackResponseDTO.Toast()
                    .setType(FeishuCallbackResponseDTO.Toast.ToastType.INFO.getValue())
                    .setContent("未知事件，请联系管理员处理"));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleClickButton(String openId, String openMessageId, FeishuCallbackResponseDTO response, String token) {
        UserPromptSubmissionsDO userPromptSubmissionsDO = userPromptSubmissionsMapper.selectOne(new LambdaQueryWrapper<UserPromptSubmissionsDO>()
                .eq(UserPromptSubmissionsDO::getMessageId, openMessageId));
        if (Objects.isNull(userPromptSubmissionsDO)) {
            // 表单是空的
            response.setToast(new FeishuCallbackResponseDTO.Toast()
                    .setType(FeishuCallbackResponseDTO.Toast.ToastType.WARNING.getValue())
                    .setContent("表单为空，请重新填写"));
        } else {
            // 表单有值，设置提示词
            userPromptSubmissionsDO.setSubmitted(UserPromptSubmissionsDO.Status.SUBMITTED.getValue());
            userPromptSubmissionsMapper.updateById(userPromptSubmissionsDO);
            UserPromptDO userPromptDO = userPromptMapper.selectOne(new LambdaQueryWrapper<UserPromptDO>()
                    .eq(UserPromptDO::getOpenId, openId));
            if (Objects.isNull(userPromptDO)) {
                userPromptMapper.insert(new UserPromptDO()
                        .setOpenId(openId)
                        .setPromptId(userPromptSubmissionsDO.getPromptId()));
            } else {
                userPromptDO.setPromptId(userPromptSubmissionsDO.getPromptId());
                userPromptMapper.updateById(userPromptDO);
            }
            response.setToast(new FeishuCallbackResponseDTO.Toast()
                    .setType(FeishuCallbackResponseDTO.Toast.ToastType.SUCCESS.getValue())
                    .setContent("提交成功！"));
            delayRenewCardAsync(userPromptSubmissionsDO.getPromptId(), token);
        }
    }

    private void delayRenewCardAsync(Long promptId, String token) {
        PromptDO promptDO = promptMapper.selectById(promptId);
        String newCardTemplate = """
                {
                    "schema": "2.0",
                    "config": {
                        "update_multi": true,
                        "style": {
                            "text_size": {
                                "normal_v2": {
                                    "default": "normal",
                                    "pc": "normal",
                                    "mobile": "heading"
                                }
                            }
                        }
                    },
                    "body": {
                        "direction": "horizontal",
                        "horizontal_spacing": "8px",
                        "vertical_spacing": "8px",
                        "horizontal_align": "left",
                        "vertical_align": "top",
                        "padding": "12px 12px 12px 12px",
                        "elements": [
                            {
                                "tag": "column_set",
                                "horizontal_align": "left",
                                "columns": [
                                    {
                                        "tag": "column",
                                        "width": "weighted",
                                        "elements": [
                                            {
                                                "tag": "markdown",
                                                "content": "选择提示词",
                                                "text_align": "left",
                                                "text_size": "normal_v2"
                                            }
                                        ],
                                        "vertical_spacing": "8px",
                                        "horizontal_align": "left",
                                        "vertical_align": "top",
                                        "weight": 1
                                    },
                                    {
                                        "tag": "column",
                                        "width": "weighted",
                                        "elements": [
                                            {
                                                "tag": "select_static",
                                                "placeholder": {
                                                    "tag": "plain_text",
                                                    "content": "请选择"
                                                },
                                                "options": [
                                                    {
                                                        "text": {
                                                            "tag": "plain_text",
                                                            "content": "%s"
                                                        },
                                                        "value": "%s"
                                                    }
                                                ],
                                                "type": "default",
                                                "width": "default",
                                                "disabled": true,
                                                "initial_option": "%s"
                                            }
                                        ],
                                        "vertical_spacing": "8px",
                                        "horizontal_align": "left",
                                        "vertical_align": "top",
                                        "weight": 1
                                    }
                                ],
                                "margin": "0px 0px 0px 0px"
                            },
                            {
                                "tag": "button",
                                "text": {
                                    "tag": "plain_text",
                                    "content": "已提交"
                                },
                                "type": "primary",
                                "width": "default",
                                "size": "medium",
                                "disabled": true,
                                "behaviors": [
                                    {
                                        "type": "callback",
                                        "value": {
                                            "": ""
                                        }
                                    }
                                ],
                                "margin": "0px 0px 0px 0px"
                            }
                        ]
                    },
                    "header": {
                        "title": {
                            "tag": "plain_text",
                            "content": "请设置您的常用提示词"
                        },
                        "subtitle": {
                            "tag": "plain_text",
                            "content": ""
                        },
                        "template": "wathet",
                        "padding": "12px 12px 12px 12px"
                    }
                }
                """;
        String newCard = String.format(newCardTemplate, promptDO.getAct(), promptId, promptId);
        feishuClient.delayRenewCard(newCard, token);
    }

    private void handleSelectStatic(String openId, String option, String openMessageId) {
        // 保存用户选择
        UserPromptSubmissionsDO userPromptSubmissionsDO = new UserPromptSubmissionsDO();
        userPromptSubmissionsDO.setOpenId(openId);
        userPromptSubmissionsDO.setMessageId(openMessageId);
        long promptId = Long.parseLong(option);
        userPromptSubmissionsDO.setPromptId(promptId);
        userPromptSubmissionsMapper.insert(userPromptSubmissionsDO);
    }

    public void handleMessage(FeishuP2pChatDTO feishuP2pChatDTO) {
        FeishuP2pChatDTO.Header header = feishuP2pChatDTO.getHeader();
        long createTime = Long.parseLong(header.getCreateTime());
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime createTimeLDT = Instant.ofEpochMilli(createTime)
                .atZone(zoneId)
                .toLocalDateTime();
        if (createTimeLDT.isBefore(LocalDateTime.now().minusMinutes(5))) {
            // 超过5分钟的消息不处理
            return;
        }
        FeishuP2pChatDTO.Event event = feishuP2pChatDTO.getEvent();
        FeishuP2pChatDTO.Message message = event.getMessage();
        FeishuP2pChatDTO.Sender sender = event.getSender();
        String eventType = header.getEventType();
        // 判断是否为接收到用户消息
        if (StringUtils.equals("im.message.receive_v1", eventType)) {
            String eventId = header.getEventId();
            FeishuP2pChatDTO.Message.Content content = message.getContent();
            String contentText = content.getText().replaceAll("@_user_1 ", "");
            FeishuP2pChatDTO.SenderId senderId = sender.getSenderId();
            String senderOpenId = senderId.getOpenId();

            // 对于同一个事件，只处理一次
            String eventKey = FeishuConstants.getFeishuEventKey(eventId);
            if (redisUtils.increment(eventKey) != 1) {
                log.info("事件重复，不处理，eventId: {}", eventId);
                return;
            } else {
                redisUtils.expire(eventKey, 60 * 60 * 1000, TimeUnit.MILLISECONDS);
            }

            // 私聊直接回复
            if (StringUtils.equals("p2p", message.getChatType())) {
                String receiveType = ReceiveType.OPEN_ID.getValue();
                try {
                    UserInfoContext.setUserInfo(new UserInfo()
                            .setReceiveId(senderOpenId)
                            .setReceiveType(receiveType));
                    handleP2pMessage(message.getMessageId(), contentText, receiveType, senderOpenId);
                } finally {
                    UserInfoContext.removeUserInfo();
                }
            }
        }
    }

    private void handleP2pMessage(String messageId, String contentText, String receiveType, String receiveId) {
        FeishuP2pMessageStrategy strategy = FeishuP2pMessageStrategyFactory.getStrategy(contentText);
        if (Objects.isNull(strategy)) {
            // 一般是输入为空
            throw new FeishuP2pException("请检查输入是否为空", receiveId, receiveType);
        }
        strategy.handleMessage(contentText, messageId);
    }

}
