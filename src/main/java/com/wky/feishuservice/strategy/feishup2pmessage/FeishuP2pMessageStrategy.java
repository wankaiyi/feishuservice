package com.wky.feishuservice.strategy.feishup2pmessage;

/**
 * @author wky
 * @date 2024/11/22
 */
public interface FeishuP2pMessageStrategy {

    void handleMessage(String contentText, String messageId);

    boolean isMatch(String contentText);
}