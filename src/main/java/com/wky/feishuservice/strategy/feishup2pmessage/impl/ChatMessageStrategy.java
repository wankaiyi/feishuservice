package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenAiClient;
import com.wky.feishuservice.constants.OpenAiConstants;
import com.wky.feishuservice.enumurations.FeishuP2pPrefix;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.exceptions.OpenAiException;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 处理聊天消息策略
 *
 * @author wky
 * @date 2024/11/22
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageStrategy implements FeishuP2pMessageStrategy {

    private final RedissonClient redissonClient;
    private final OpenAiClient openAiClient;
    private final FeishuClient feishuClient;
    @Resource
    private ThreadPoolTaskExecutor openaiChatThreadPool;

    @Override
    public void handleMessage(String contentText, String messageId) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveId = userInfo.getReceiveId();
        String receiveType = userInfo.getReceiveType();
        String key = OpenAiConstants.getOpenaiChatQueueRedisKey(receiveId);
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(key);
        queue.addAsync(contentText);
        RLock lock = redissonClient.getLock(OpenAiConstants.getOpenaiChatLockKey(receiveId));
        Map<String, String> MDCMap = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> {
            try {
                MDC.setContextMap(MDCMap);
                // 获取锁，获取成功后有看门狗续命
                if (lock.tryLock()) {
                    while (!queue.isEmpty()) {
                        String text = queue.take();
                        try {
                            ChatResponseBO chatResponseBO = openAiClient.chat(receiveId, text);
                            feishuClient.sendP2pMsg(chatResponseBO, receiveId, receiveType, "post", messageId);
                        } catch (OpenAiException e) {
                            log.error("获取chatgpt结果失败 error:", e);
                            feishuClient.handelP2pException(new FeishuP2pException(e.getMessage(), receiveId, receiveType));
                        } catch (FeishuP2pException e) {
                            log.error("飞书发送消息失败 error:", e);
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

    @Override
    public boolean isMatch(String contentText) {
        boolean match = StringUtils.isNoneBlank(contentText) && FeishuP2pPrefix.notInFeishuP2pPrefixes(contentText);
        if (match) {
            log.info("匹配到chatgpt聊天策略");
        }
        return match;
    }
}
