package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author wky
 * @date 2024/12/26
 */
@Component
@RequiredArgsConstructor
public class PromptConfigStrategy implements FeishuP2pMessageStrategy {

    private final FeishuClient feishuClient;


    @Override
    public void handleMessage(String contentText, String messageId) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveId = userInfo.getReceiveId();
        String receiveType = userInfo.getReceiveType();
        feishuClient.sendPromptConfigCard(receiveId, receiveType,  messageId);
    }

    @Override
    public boolean isMatch(String contentText) {
        return StringUtils.startsWith(contentText, "#配置");
    }
}
