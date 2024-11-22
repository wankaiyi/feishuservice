package com.wky.feishuservice.strategy.feishup2pmessage;

import com.wky.feishuservice.model.dto.FeishuP2pChatDTO;

/**
 * @author wky
 * @date 2024/11/22
 */
public interface FeishuP2pMessageStrategy {

    void handleMessage(String contentText, String receiveId, String receiveType, FeishuP2pChatDTO.Message message);

    boolean isMatch(String contentText);
}