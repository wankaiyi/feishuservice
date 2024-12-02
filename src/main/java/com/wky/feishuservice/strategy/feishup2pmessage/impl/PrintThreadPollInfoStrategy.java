package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.config.ThreadPoolConfig;
import com.wky.feishuservice.enumurations.FeishuP2pPrefix;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 处理聊天消息策略
 *
 * @author wky
 * @date 2024/11/22
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrintThreadPollInfoStrategy implements FeishuP2pMessageStrategy {

    public static final String PREFIX = FeishuP2pPrefix.THREAD_POOL_INFO.getPrefix();

    @Resource
    private ThreadPoolTaskExecutor openaiChatThreadPool;
    private final FeishuClient feishuClient;

    @Override
    public void handleMessage(String contentText, String messageId) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveId = userInfo.getReceiveId();
        String receiveType = userInfo.getReceiveType();
        String threadPoolInfo = ThreadPoolConfig.doPrintThreadPoolConfig(openaiChatThreadPool.getThreadPoolExecutor());
        feishuClient.sendTpP2pMessage(threadPoolInfo, receiveId, receiveType, messageId);
    }

    @Override
    public boolean isMatch(String contentText) {
        return StringUtils.startsWith(contentText, PREFIX);
    }
}
