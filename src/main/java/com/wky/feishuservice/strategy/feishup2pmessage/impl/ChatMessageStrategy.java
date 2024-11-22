package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenaiClient;
import com.wky.feishuservice.constants.OpenAiConstants;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.exceptions.OpenAiException;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.dto.FeishuP2pChatDTO;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

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
    private final OpenaiClient openaiClient;
    private final FeishuClient feishuClient;
    @Resource
    private ThreadPoolTaskExecutor openaiChatThreadPool;

    @Override
    public void handleMessage(String contentText, String receiveId, String receiveType, FeishuP2pChatDTO.Message message) {
        String key = OpenAiConstants.getOpenaiChatQueueRedisKey(receiveId);
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(key);
        queue.addAsync(contentText);
        RLock lock = redissonClient.getLock(OpenAiConstants.getOpenaiChatLockKey(receiveId));
        CompletableFuture.runAsync(() -> {
            try {
                // 获取锁，获取成功后有看门狗续命
                if (lock.tryLock()) {
                    while (!queue.isEmpty()) {
                        String text = queue.take();
                        try {
                            ChatResponseBO chatResponseBO = openaiClient.chat(receiveId, text);
                            feishuClient.sendP2pMsg(chatResponseBO, receiveId, receiveType, "post", message.getMessageId());
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

    @Override
    public boolean isMatch(String contentText) {
        return StringUtils.isNotBlank(contentText) && !contentText.startsWith("#");
    }
}
