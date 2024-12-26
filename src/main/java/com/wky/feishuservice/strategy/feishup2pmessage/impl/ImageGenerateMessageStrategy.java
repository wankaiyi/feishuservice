package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenAiClient;
import com.wky.feishuservice.enumurations.FeishuP2pPrefix;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.model.dto.ImageGenerateResponseDTO;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author wky
 * @date 2024/11/22
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerateMessageStrategy implements FeishuP2pMessageStrategy {

    private static final String PREFIX = FeishuP2pPrefix.IMAGE_GENERATE.getPrefix();
    private final OpenAiClient openAiClient;
    private final FeishuClient feishuClient;

    @Override
    public void handleMessage(String contentText, String messageId) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveId = userInfo.getReceiveId();
        String receiveType = userInfo.getReceiveType();
        ImageGenerateResponseDTO imageGenerateResponseDTO = openAiClient.generateImage(contentText.substring(PREFIX.length()));
        Optional.ofNullable(imageGenerateResponseDTO).orElseThrow(() -> new FeishuP2pException("图片生成失败", receiveId, receiveType));
        List<ImageGenerateResponseDTO.DataItem> data = imageGenerateResponseDTO.getData();
        data.forEach(base64 -> {
            // 上传图片到飞书
            String imageKey = feishuClient.uploadImage(base64.getB64Json(), receiveId, receiveType);
            // 发送飞书图片消息
            feishuClient.sendImageMessage(imageKey, receiveId, receiveType, messageId);
        });
    }

    @Override
    public boolean isMatch(String contentText) {
        boolean match = StringUtils.startsWith(contentText, PREFIX);
        if (match) {
            log.info("匹配到图片生成策略");
        }
        return match;
    }

}
