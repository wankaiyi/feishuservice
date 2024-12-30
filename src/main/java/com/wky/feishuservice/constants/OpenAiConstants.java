package com.wky.feishuservice.constants;

/**
 * @author wky
 * @date 2024/11/22
 */
public class OpenAiConstants {

    private static final String OPENAI_CHAT_QUEUE_REDIS_KEY = "openai:chat:queue:%s";

    private static final String OPENAI_CHAT_LOCK_KEY = "openai:chat:lock:%s";

    private static final String OPENAI_PROXY_BASE_URL = "https://openai-proxy-openai-proxy-jrrrbwnaqt.us-west-1.fcapp.run";
    //    private final String chatUrl = "https://api.openai.com/v1/chat/completions";
    public static final String CHAT_URL = OPENAI_PROXY_BASE_URL + "/v1/chat/completions";

    public final static String IMAGE_GENERATION_URL = OPENAI_PROXY_BASE_URL + "/v1/images/generations";


    public static String getOpenaiChatQueueRedisKey(String openId) {
        return String.format(OPENAI_CHAT_QUEUE_REDIS_KEY, openId);
    }

    public static String getOpenaiChatLockKey(String openId) {
        return String.format(OPENAI_CHAT_LOCK_KEY, openId);
    }

    public static String PREDICT_QUESTION_PROMPT ="我想让你作为一个OpenAI助手，帮助根据给定的上下文预测用户可能提出的下一个问题。请确保所有的回答使用中文。如果有多个可能的问题，请使用空格隔开，且最多提供3个预测。如果检测到用户可能不会再提问，则仅返回“无”。请注意，返回结果应严格按照指定格式，如“java有什么优势\njava相比其他语言有什么优点\njava的性能”或“无”，不要包含任何其他多余的文字或标点符号,每个问题用回车换行\n分隔开。";
}
