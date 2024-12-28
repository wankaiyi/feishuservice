package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.cache.ChatMsgCache;
import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.enumurations.FeishuP2pPrefix;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 刷新记忆消息策略
 *
 * @author wky
 * @date 2024/11/22
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshMemoryMessageStrategy implements FeishuP2pMessageStrategy {

    private final ChatMsgCache chatMsgCache;
    private final FeishuClient feishuClient;

    @Override
    public void handleMessage(String contentText, String messageId) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveType = userInfo.getReceiveType();
        String receiveId = userInfo.getReceiveId();
        chatMsgCache.refreshCache(receiveId);
        feishuClient.sendRefreshSuccessMsg(receiveId, receiveType, messageId);
    }

    @Override
    public boolean isMatch(String contentText) {
        boolean match = StringUtils.startsWith(contentText, FeishuP2pPrefix.REFRESH_MEMORY.getPrefix());
        if (match) {
            log.info("匹配到刷新上下文策略");
        }
        return match;
    }
}
