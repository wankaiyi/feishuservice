package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.enumurations.FeishuP2pPrefix;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.service.FeishuMessageService;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
