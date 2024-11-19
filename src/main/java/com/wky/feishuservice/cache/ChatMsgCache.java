package com.wky.feishuservice.cache;

import com.wky.feishuservice.model.dto.ChatRequestDTO;
import com.wky.feishuservice.utils.JacksonUtils;
import com.wky.feishuservice.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author wky
 * @date 2024/11/11
 */
@Component
@RequiredArgsConstructor
public class ChatMsgCache {

    private final RedissonClient redissonClient;
    private final RedisUtils redisUtils;

    public List<ChatRequestDTO.Message> getMsgCache(String openId) {
        String key = getKey(openId);
        return redissonClient.getDeque(key).stream().map(str -> JacksonUtils.deserialize(str.toString(), ChatRequestDTO.Message.class)).toList();
    }

    private String getKey(String openId) {
        return "chatgpt:history:" + openId;
    }

    public void addMsgCache(String openId, ChatRequestDTO.Message message) {
        String key = getKey(openId);
        RDeque<String> messages = redissonClient.getDeque(key);
        messages.addLast(JacksonUtils.serialize(message));

        int msgCacheSize = Integer.parseInt(
                Optional
                        .ofNullable(redisUtils.get("chatgpt:msg_cache_size"))
                        .orElse("5")
        );
        while (messages.size() > msgCacheSize) {
            messages.pollFirst();
        }
        // 历史记录保留三个小时
        redisUtils.expire(key, 60 * 60 * 3 * 1000, TimeUnit.MILLISECONDS);
    }

}
