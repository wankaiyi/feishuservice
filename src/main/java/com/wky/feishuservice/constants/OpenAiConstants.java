package com.wky.feishuservice.constants;

/**
 * @author wky
 * @date 2024/11/22
 */
public class OpenAiConstants {

    private static final String OPENAI_CHAT_QUEUE_REDIS_KEY = "openai:chat:queue:%s";

    private static final String OPENAI_CHAT_LOCK_KEY = "openai:chat:lock:%s";


    public static String getOpenaiChatQueueRedisKey(String openId) {
        return String.format(OPENAI_CHAT_QUEUE_REDIS_KEY, openId);
    }

    public static String getOpenaiChatLockKey(String openId) {
        return String.format(OPENAI_CHAT_LOCK_KEY, openId);
    }
}
