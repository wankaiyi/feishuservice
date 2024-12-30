package com.wky.feishuservice.strategy.feishucardbutton.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenAiClient;
import com.wky.feishuservice.enumurations.FeishuCardButtonType;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.model.dto.FeishuCallbackResponseDTO;
import com.wky.feishuservice.model.dto.FeishuP2pResponseDTO;
import com.wky.feishuservice.service.FeishuMessageService;
import com.wky.feishuservice.strategy.feishucardbutton.FeishuCardButtonStrategy;
import com.wky.feishuservice.utils.JacksonUtils;
import com.wky.feishuservice.utils.UserInfoContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NextQuestionProcessStrategy implements FeishuCardButtonStrategy {
    private final FeishuMessageService feishuMessageService;
    private final FeishuClient feishuClient;
    private final OpenAiClient openAiClient;
    @Resource
    private ThreadPoolTaskExecutor openaiChatThreadPool;

    @Override
    public void handle(String openMessageId, FeishuCallbackResponseDTO response, String token, String question) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveId = userInfo.getReceiveId();
        String receiveType = userInfo.getReceiveType();
        //先发送消息给用户，告诉用户他问了什么问题
        Map<String, Object> map = new HashMap<>();
        map.put("text", question);
        String format = String.format(defalutCard, JacksonUtils.serialize(map));
        FeishuP2pResponseDTO feishuP2pResponseDTO = feishuClient.sendFeishuP2pMsg(format, receiveId, "open_id", "interactive", null);
        //处理用户发起的问题
        feishuMessageService.processUserQuestion(receiveId, question, receiveType, feishuP2pResponseDTO.getData().getMessageId());
    }

    private String defalutCard = """
            {
                "type":"template",
                "data":{
                    "template_id":"AAqS71w5lSWdk",
                    "template_version_name":"1.0.0",
                    "template_variable":%s
                }
            }
            """;

    @Override
    public boolean isMatch(FeishuCardButtonType type) {
        return FeishuCardButtonType.NEXT_QUESTION_ACCEPT.equals(type);
    }
}
