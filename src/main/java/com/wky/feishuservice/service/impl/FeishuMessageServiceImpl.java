package com.wky.feishuservice.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenAiClient;
import com.wky.feishuservice.constants.FeishuConstants;
import com.wky.feishuservice.constants.OpenAiConstants;
import com.wky.feishuservice.enumurations.FeishuCallbackActionTag;
import com.wky.feishuservice.enumurations.FeishuCardButtonType;
import com.wky.feishuservice.enumurations.ReceiveType;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.exceptions.OpenAiException;
import com.wky.feishuservice.mapper.PromptMapper;
import com.wky.feishuservice.mapper.UserPromptSubmissionsMapper;
import com.wky.feishuservice.model.bo.ChatMessageBo;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.model.dto.FeishuCallbackRequestDTO;
import com.wky.feishuservice.model.dto.FeishuCallbackResponseDTO;
import com.wky.feishuservice.model.dto.FeishuP2pChatDTO;
import com.wky.feishuservice.model.po.PromptDO;
import com.wky.feishuservice.model.po.UserPromptSubmissionsDO;
import com.wky.feishuservice.service.FeishuMessageService;
import com.wky.feishuservice.strategy.feishucardbutton.FeishuCardButtonStrategyFactory;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategyFactory;
import com.wky.feishuservice.utils.JacksonUtils;
import com.wky.feishuservice.utils.RedisUtils;
import com.wky.feishuservice.utils.SpringContextUtil;
import com.wky.feishuservice.utils.UserInfoContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author wky
 * @date 2024/11/11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeishuMessageServiceImpl implements FeishuMessageService {
    /**
     * 事件重复处理的key的过期时间
     */
    @Value("${feishu.event.duplcation.expire.time:3600000}")
    private long eventHandlerExpireTime;
    @Value("${feishu.predictQuestion.enable:false}")
    private boolean predictQuestion;

    private final RedisUtils redisUtils;
    private final UserPromptSubmissionsMapper userPromptSubmissionsMapper;
    private final RedissonClient redissonClient;
    private final PromptMapper promptMapper;
    private final FeishuClient feishuClient;
    @Resource
    private ThreadPoolTaskExecutor openaiChatThreadPool;
    private final OpenAiClient openAiClient;

    @Override
    public JSONObject processFeishuNotice(FeishuP2pChatDTO feishuP2pChatDTO) {
        log.info("接收到飞书事件，请求体：{}", feishuP2pChatDTO);
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
        log.info("接收到飞书回调，请求体：{}", feishuCallbackRequestDTO);
        FeishuCallbackResponseDTO response = new FeishuCallbackResponseDTO();
        if (StringUtils.equals("url_verification", feishuCallbackRequestDTO.getType())) {
            response.setChallenge(feishuCallbackRequestDTO.getChallenge());
            return response;
        }
        // 处理回调消息
        try {
            String openId = feishuCallbackRequestDTO.getEvent().getOperator().getOpenId();
            UserInfoContext.setUserInfo(new UserInfo()
                    .setReceiveId(openId));
            handleCallback(feishuCallbackRequestDTO, response);
        } finally {
            UserInfoContext.removeUserInfo();
        }
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
            redisUtils.expire(eventKey, eventHandlerExpireTime, TimeUnit.MILLISECONDS);
        }
        String actionTag = feishuCallbackRequestDTO.getEvent().getAction().getTag();
        String openId = UserInfoContext.getUserInfo().getReceiveId();
        String openMessageId = feishuCallbackRequestDTO.getEvent().getContext().getOpenMessageId();

        RLock lock = redissonClient.getLock(FeishuConstants.getFeishuPromptOptionLockKey(openId));
        try {
            if (lock.tryLock()) {
                if (StringUtils.equals(FeishuCallbackActionTag.SELECT_STATIC.getValue(), actionTag)) {
                    handleSelectStatic(openId,
                            feishuCallbackRequestDTO.getEvent().getAction().getOption(),
                            openMessageId,
                            response);
                } else if (StringUtils.equals(FeishuCallbackActionTag.BUTTON.getValue(), actionTag)) {
                    FeishuMessageServiceImpl self = SpringContextUtil.getBean(this.getClass());
                    FeishuCallbackRequestDTO.Event.Action action = feishuCallbackRequestDTO.getEvent().getAction();
                    Map<String, String> value = action.getValue();
                    self.handleClickButton(
                            openMessageId,
                            response,
                            feishuCallbackRequestDTO.getEvent().getToken(),
                            FeishuCardButtonType.valueOf(feishuCallbackRequestDTO.getEvent().getAction().getValue().get("type")),
                            value.get("question")
                    );
                } else {
                    log.info("未知事件，eventId: {}, actionTag: {}", eventId, actionTag);
                    response.setToast(new FeishuCallbackResponseDTO.Toast()
                            .setType(FeishuCallbackResponseDTO.Toast.ToastType.INFO.getValue())
                            .setContent("未知事件，请联系管理员处理"));
                }
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
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleClickButton(String openMessageId, FeishuCallbackResponseDTO response, String token, FeishuCardButtonType type, String question) {
        FeishuCardButtonStrategyFactory.getStrategy(type).handle(openMessageId, response, token, question);
    }

    @Override
    public void delayRenewCardAsync(Long promptId, String token) {
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
        String newCard;
        if (Objects.isNull(promptDO)) {
            newCard = String.format(newCardTemplate, "", 1, 1);
        } else {
            newCard = String.format(newCardTemplate, promptDO.getAct(), promptId, promptId);
        }
        feishuClient.delayRenewCard(newCard, token);
    }

    @Override
    public void processUserQuestion(String receiveId, String text, String receiveType, String messageId) {
        log.info("messageId:{}", messageId);
        String key = OpenAiConstants.getOpenaiChatQueueRedisKey(receiveId);
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(key);
        queue.addAsync(JacksonUtils.serialize(new ChatMessageBo(text, messageId)));
        RLock lock = redissonClient.getLock(OpenAiConstants.getOpenaiChatLockKey(receiveId));
        Map<String, String> MDCMap = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> {
            try {
                MDC.setContextMap(MDCMap);
                // 获取锁，获取成功后有看门狗续命
                if (lock.tryLock()) {
                    while (!queue.isEmpty()) {
                        String chatMessageBoStr = queue.take();
                        ChatMessageBo chatMessageBo = JacksonUtils.deserialize(chatMessageBoStr, ChatMessageBo.class);
                        String question = chatMessageBo.getText();
                        try {

                            //如果开启了预测问题
                            if (predictQuestion){
                                CompletableFuture<ChatResponseBO> aiTask = CompletableFuture.supplyAsync(() -> openAiClient.chat(receiveId, question), openaiChatThreadPool);
                                CompletableFuture<ChatResponseBO> questionTask = CompletableFuture.supplyAsync(() -> openAiClient.getPredictNextQuestion(receiveId, question), openaiChatThreadPool);
                                //并行请求，加快速度
                                CompletableFuture.allOf(aiTask, questionTask).thenRun(() -> {
                                    try {
                                        ChatResponseBO chatResponseBO = aiTask.get();
                                        ChatResponseBO predictNextQuestion = questionTask.get();
                                        feishuClient.sendP2PPredictQuestion(predictNextQuestion.getContent(), receiveId, receiveType, "interactive", feishuClient.sendP2pMsg(chatResponseBO, receiveId, receiveType, "post", chatMessageBo.getMessageId()).getData().getMessageId());
                                    } catch (InterruptedException | ExecutionException e) {
                                        log.error("处理用户问题失败 error:", e);
                                    }
                                }).join();
                            }
                            else {
                                feishuClient.sendP2pMsg(openAiClient.chat(receiveId, question), receiveId, receiveType, "post", chatMessageBo.getMessageId());
                            }

                        } catch (OpenAiException e) {
                            log.error("获取chatgpt结果失败 error:", e);
                            feishuClient.handleP2pException(new FeishuP2pException(e.getMessage(), receiveId, receiveType));
                        } catch (FeishuP2pException e) {
                            log.error("飞书发送消息失败 error:", e);
                            feishuClient.handleP2pException(e);
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.warn("线程被中断，结束任务。queue key: {}", key);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
                MDC.clear();
            }
        }, openaiChatThreadPool);

    }

    private void handleSelectStatic(String openId, String option, String openMessageId, FeishuCallbackResponseDTO response) {
        // 保存用户选择
        UserPromptSubmissionsDO userPromptSubmissionsDO = userPromptSubmissionsMapper.selectOne(new LambdaQueryWrapper<UserPromptSubmissionsDO>()
                .eq(UserPromptSubmissionsDO::getMessageId, openMessageId));
        long promptId = Long.parseLong(option);
        if (Objects.nonNull(userPromptSubmissionsDO)) {
            Short submitted = userPromptSubmissionsDO.getSubmitted();
            if (UserPromptSubmissionsDO.Status.SUBMITTED.getValue() == submitted) {
                // 表单已提交
                response.setToast(new FeishuCallbackResponseDTO.Toast()
                        .setType(FeishuCallbackResponseDTO.Toast.ToastType.WARNING.getValue())
                        .setContent("表单已提交，请勿重复提交"));
                return;
            }
            userPromptSubmissionsDO.setPromptId(promptId);
            userPromptSubmissionsMapper.updateById(userPromptSubmissionsDO);
        } else {
            userPromptSubmissionsDO = new UserPromptSubmissionsDO();
            userPromptSubmissionsDO.setOpenId(openId);
            userPromptSubmissionsDO.setMessageId(openMessageId);
            userPromptSubmissionsDO.setPromptId(promptId);
            userPromptSubmissionsMapper.insert(userPromptSubmissionsDO);
        }
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
