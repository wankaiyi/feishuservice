package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenAiClient;
import com.wky.feishuservice.constants.OpenAiConstants;
import com.wky.feishuservice.enumurations.FeishuP2pPrefix;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.exceptions.OpenAiException;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.service.FeishuMessageService;
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

    private final FeishuMessageService feishuMessageService;

    @Override
    public void handleMessage(String contentText, String messageId) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveId = userInfo.getReceiveId();
        String receiveType = userInfo.getReceiveType();
        feishuMessageService.processUserQuestion(receiveId, contentText, receiveType, messageId);
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
