package com.wky.feishuservice.strategy.feishucardbutton.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenAiClient;
import com.wky.feishuservice.enumurations.FeishuCardButtonType;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.model.dto.ChatResponseDTO;
import com.wky.feishuservice.model.dto.FeishuCallbackResponseDTO;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        Map<String,Object> map=new HashMap<>();
        map.put("text",question);
        FeishuClient self = SpringUtil.getBean(FeishuClient.class);
        String format = String.format(defalutCard, JacksonUtils.serialize(map));
        self.sendFeishuP2pMsg(format,receiveId,"open_id","interactive",null);
        CompletableFuture<ChatResponseBO> aiTask = CompletableFuture.supplyAsync(() -> {
            ChatResponseBO chatResponseBO = openAiClient.chat(receiveId, question);
            return chatResponseBO;
        }, openaiChatThreadPool);
        CompletableFuture<ChatResponseBO> questionTask = CompletableFuture.supplyAsync(() -> {
            ChatResponseBO predictNextQuestion =openAiClient.getPredictNextQuestion(receiveId,question);
            return predictNextQuestion;
        }, openaiChatThreadPool);
        CompletableFuture.allOf(aiTask,questionTask).thenRun(()->{
            try {
                ChatResponseBO chatResponseBO = aiTask.get();
                feishuClient.sendP2pMsg(chatResponseBO, receiveId, receiveType, "post", openMessageId);
                ChatResponseBO predictNextQuestion = questionTask.get();
                feishuClient.sendP2PPredictQuestion(predictNextQuestion, receiveId, receiveType, "interactive", openMessageId);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).join();
    }
    private String defalutCard= """
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
