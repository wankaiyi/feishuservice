package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.cache.ChatMsgCache;
import com.wky.feishuservice.model.dto.FeishuP2pChatDTO;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import lombok.RequiredArgsConstructor;
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
public class RefreshMemoryMessageStrategy implements FeishuP2pMessageStrategy {

    private final ChatMsgCache chatMsgCache;

    @Override
    public void handleMessage(String contentText, String receiveId, String receiveType, FeishuP2pChatDTO.Message message) {
        chatMsgCache.refreshCache(receiveId);
    }

    @Override
    public boolean isMatch(String contentText) {
        return StringUtils.startsWith(contentText, "#reset");
    }
}
