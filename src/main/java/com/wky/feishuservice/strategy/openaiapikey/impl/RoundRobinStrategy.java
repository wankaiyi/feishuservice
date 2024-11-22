package com.wky.feishuservice.strategy.openaiapikey.impl;

import com.wky.feishuservice.config.OpenAiConfig;
import com.wky.feishuservice.strategy.openaiapikey.ApiKeySelectionStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RList;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 轮询策略
 *
 * @author wky
 * @date 2024/11/11
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "openai.apikey.strategy",
        havingValue = "round-robin"
)
public class RoundRobinStrategy implements ApiKeySelectionStrategy {

    private final RedissonClient redissonClient;
    private final OpenAiConfig openaiConfig;
    private static final String API_KEY_LIST_KEY = "openai:round_robin_strategy:api_key_list";
    private static final String CURRENT_INDEX_KEY = "openai:round_robin_strategy:current_index";
    private static final String LUA_SCRIPT = """
                local listKey = KEYS[1]
                local indexKey = KEYS[2]
                local currentIndex = tonumber(redis.call('GET', indexKey) or 0)
                local listLength = redis.call('LLEN', listKey)
                if listLength == 0 then return nil end
                currentIndex = (currentIndex % listLength) + 1
                local apiKey = redis.call('LINDEX', listKey, currentIndex - 1)
                redis.call('SET', indexKey, currentIndex)
                return apiKey
                """;
    @Override
    public String selectApiKey() {
        return redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        LUA_SCRIPT,
                        // 返回字符串类型
                        RScript.ReturnType.STATUS,
                        List.of(API_KEY_LIST_KEY, CURRENT_INDEX_KEY));
    }

    @PostConstruct
    public void init() throws IllegalAccessException {
        List<String> apiKeys = openaiConfig.getApiKeys();
        if (CollectionUtils.isEmpty(apiKeys)) {
            throw new IllegalAccessException("apikeys不能为空");
        }
        RList<String> list = redissonClient.getList(API_KEY_LIST_KEY);
        // 不存在就添加
        apiKeys.forEach(apiKey -> {
            if (!list.contains(apiKey)) {
                list.add(apiKey);
            }
        });
        // 删除多余的apikey
        list.forEach(apiKey -> {
            if (!apiKeys.contains(apiKey)) {
                list.remove(apiKey);
            }
        });
        redissonClient.getBucket(CURRENT_INDEX_KEY).trySetAsync(0);
    }

}
