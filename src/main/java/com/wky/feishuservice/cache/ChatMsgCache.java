package com.wky.feishuservice.cache;

import com.wky.feishuservice.model.dto.ChatRequestDTO;
import com.wky.feishuservice.utils.JacksonUtils;
import com.wky.feishuservice.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RDeque;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author wky
 * @date 2024/11/11
 */
@Component
@RequiredArgsConstructor
public class ChatMsgCache {
    /**
     * 上下文缓存大小
     */
    @Value("${chatgpt.msg.cache.size:10}")
    private int msgCacheSize;
    /**
     * 上下文缓存过期时间
     */
    @Value("${chatgpt.msg.expire.time:10800000}")
    private long msgExpireTime;

    private final RedissonClient redissonClient;
    private final RedisUtils redisUtils;

    public List<ChatRequestDTO.Message> getMsgCache(String openId) {
        String key = getKey(openId);
        RList<String> messages = redissonClient.getList(key);
        return messages.range(0, messages.size() - 1).stream().map(str -> JacksonUtils.deserialize(str, ChatRequestDTO.Message.class)).toList();
    }

    private String getKey(String openId) {
        return "chatgpt:history:" + openId;
    }

    public void addMsgCache(String openId, ChatRequestDTO.Message message) {
        String key = getKey(openId);
        RDeque<String> messages = redissonClient.getDeque(key);
        messages.addLast(JacksonUtils.serialize(message));

        while (messages.size() > msgCacheSize) {
            messages.pollFirst();
        }
        // 历史记录保留三个小时
        redisUtils.expire(key, msgExpireTime, TimeUnit.MILLISECONDS);
    }

    public void refreshCache(String openId) {
        redissonClient.getBucket(getKey(openId)).deleteAsync();
    }

}
