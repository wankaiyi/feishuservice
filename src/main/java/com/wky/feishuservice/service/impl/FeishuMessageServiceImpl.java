package com.wky.feishuservice.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.wky.feishuservice.cache.ChatMsgCache;
import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenaiClient;
import com.wky.feishuservice.enumurations.ReceiveType;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.exceptions.OpenAiException;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.dto.FeishuP2pChatDTO;
import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import com.wky.feishuservice.service.FeishuMessageService;
import com.wky.feishuservice.utils.JacksonUtils;
import com.wky.feishuservice.utils.RedisUtils;
import groovy.lang.Tuple2;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author wky
 * @date 2024/11/11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeishuMessageServiceImpl implements FeishuMessageService {

    private final FeishuClient feishuClient;
    private final RedisUtils redisUtils;
    private final OpenaiClient openaiClient;
    private final LocationServiceImpl locationServiceImpl;
    @Resource
    private ThreadPoolTaskExecutor openaiChatThreadPool;
    private final RedissonClient redissonClient;
    private final ChatMsgCache chatMsgCache;

    @Override
    public JSONObject processFeishuNotice(FeishuP2pChatDTO feishuP2pChatDTO) {
        JSONObject res = null;
        if (StringUtils.equals("url_verification", feishuP2pChatDTO.getType())) {
            res = new JSONObject();
            res.put("challenge", feishuP2pChatDTO.getChallenge());
            return res;
        }
        log.info("飞书通知接口被请求，请求体为：{}", JacksonUtils.serialize(feishuP2pChatDTO));
        //处理消息
        handleMessage(feishuP2pChatDTO);
        return res;
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
            if (Objects.nonNull(redisUtils.get("feishu:eventId:" + eventId))) {
                log.info("事件重复，不处理，eventId: {}", eventId);
                return;
            }
            //缓存event_id
            redisUtils.set("feishu:eventId:" + eventId, eventId, 60 * 60 * 1000, TimeUnit.MILLISECONDS);

            // 私聊直接回复
            if (StringUtils.equals("p2p", message.getChatType())) {
                handleP2pMessage(message, contentText, ReceiveType.OPEN_ID.getValue(), senderOpenId);
            }
        }
    }

    //处理文本消息
    private void handleP2pMessage(FeishuP2pChatDTO.Message message, String contentText, String receiveType, String receiveId) {
        if (contentText.startsWith("#天气")) {
            // 只保留中文字符
            String location = contentText.substring(3).replaceAll("[^\\u4e00-\\u9fa5]", "");
            Tuple2<LocationDO, WeatherResponseDTO> locationAndWeather = locationServiceImpl.getWeather(location);
            feishuClient.handelWeather(locationAndWeather, receiveId, receiveType, "interactive");
        } else if (contentText.startsWith("#reset")){
            // 清空记忆
            refreshMemory(receiveId);
        } else {
            handleOpenaiMsg(receiveId, receiveType, message.getMessageId(), contentText);
        }
    }

    private void refreshMemory(String receiveId) {
        chatMsgCache.refreshCache(receiveId);
    }

    private void handleOpenaiMsg(String receiveId, String receiveType, String messageId, String contentText) {
        String key = "openai:chat:queue:" + receiveId;
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(key);
        queue.addAsync(contentText);
        RLock lock = redissonClient.getLock("openai:chat:lock:" + receiveId);
        CompletableFuture.runAsync(() -> {
            try {
                // 获取锁，获取成功后有看门狗续命
                if (lock.tryLock()) {
                    while (!queue.isEmpty()) {
                        String text = queue.take();
                        try {
                            ChatResponseBO chatResponseBO = openaiClient.chat(receiveId, text);
                            feishuClient.sendP2pMsg(chatResponseBO, receiveId, receiveType, "post", messageId);
                        } catch (OpenAiException e) {
                            feishuClient.handelP2pException(new FeishuP2pException(e.getMessage(), receiveId, receiveType));
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.warn("线程被中断，结束任务。queue key: {}", key);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }, openaiChatThreadPool);
    }

}
