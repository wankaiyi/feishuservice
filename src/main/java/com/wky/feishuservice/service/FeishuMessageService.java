package com.wky.feishuservice.service;

import com.alibaba.fastjson2.JSONObject;
import com.wky.feishuservice.model.dto.FeishuCallbackRequestDTO;
import com.wky.feishuservice.model.dto.FeishuCallbackResponseDTO;
import com.wky.feishuservice.model.dto.FeishuP2pChatDTO;

/**
 * @author wky
 * @date 2024/11/11
 */
public interface FeishuMessageService {
    JSONObject processFeishuNotice(FeishuP2pChatDTO feishuP2pChatDTO);

    FeishuCallbackResponseDTO processFeishuCallback(FeishuCallbackRequestDTO feishuCallbackRequestDTO);

    void delayRenewCardAsync(Long promptId, String token);

    void processUserQuestion(String receiveId, String text, String receiveType, String messageId);
}
