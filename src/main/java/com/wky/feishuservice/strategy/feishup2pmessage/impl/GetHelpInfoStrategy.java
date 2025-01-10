package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.enumurations.FeishuP2pPrefix;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 列出可用的指令列表
 * @author chenzhihao/陈志豪
 * @date 2025-01-10 22:57:33
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetHelpInfoStrategy implements FeishuP2pMessageStrategy {
    public static final String PREFIX = FeishuP2pPrefix.GET_HELP_INFO.getPrefix();
    private final FeishuClient feishuClient;
    private final String HELP_INFO_TIP= """
            指令列表
            #image 文生图
            #reset 重置上下文
            #天气   查询天气
            #tp    查看线程池快照
            #配置   配置上下文
            #help  列出可用的指令列表
            """;
    @Override
    public void handleMessage(String contentText, String messageId) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveId = userInfo.getReceiveId();
        String receiveType = userInfo.getReceiveType();
        ChatResponseBO chatResponseBO=new ChatResponseBO();
        chatResponseBO.setContent(HELP_INFO_TIP);
        feishuClient.sendP2pMsg(chatResponseBO, receiveId, receiveType,"post", messageId);
    }

    @Override
    public boolean isMatch(String contentText) {
        boolean match = StringUtils.startsWith(contentText, PREFIX);
        if (match) {
            log.info("匹配到线程池快照查询策略");
        }
        return match;
    }
}
