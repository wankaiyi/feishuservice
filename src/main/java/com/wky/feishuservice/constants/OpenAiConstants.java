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
}
