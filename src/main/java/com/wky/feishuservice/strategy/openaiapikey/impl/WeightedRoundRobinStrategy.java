package com.wky.feishuservice.strategy.openaiapikey.impl;

import com.wky.feishuservice.config.OpenAiConfig;
import com.wky.feishuservice.strategy.openaiapikey.ApiKeySelectionStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 加权轮询策略
 *
 * @author wky
 * @date 2024/11/18
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "openai.apikey.strategy",
        havingValue = "weighted-round-robin"
)
public class WeightedRoundRobinStrategy implements ApiKeySelectionStrategy {

    private final RedissonClient redissonClient;
    private final OpenAiConfig openaiConfig;
    private static final String API_KEY_ZSET_KEY = "openai:weighted_round_robin_strategy:api_key_zset";
    private static final String LUA_SCRIPT = """
            local api_keys = redis.call('HGETALL', KEYS[1])
            if #api_keys == 0 then
                return nil
            end
                        
            local total_weight = 0
            local weighted_keys = {}
                        
            for i = 1, #api_keys, 2 do
                local key = api_keys[i]
                local weight = tonumber(api_keys[i + 1])
                if weight then
                    total_weight = total_weight + weight
                    table.insert(weighted_keys, {key = key, weight = weight})
                end
            end
                        
            local random_choice = math.random() * total_weight
                        
            local cumulative_weight = 0
            for _, entry in ipairs(weighted_keys) do
                cumulative_weight = cumulative_weight + entry.weight
                if random_choice <= cumulative_weight then
                    return entry.key
                end
            end
                        
            return nil
            """;

    @Override
    public String selectApiKey() {
        return redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        LUA_SCRIPT,
                        // 返回字符串类型
                        RScript.ReturnType.VALUE,
                        List.of(API_KEY_ZSET_KEY));
    }

    @PostConstruct
    public void init() throws IllegalAccessException {
        List<String> apiKeys = openaiConfig.getApiKeys();
        if (CollectionUtils.isEmpty(apiKeys)) {
            throw new IllegalAccessException("apikeys不能为空");
        }
        RMap<String, Integer> map = redissonClient.getMap(API_KEY_ZSET_KEY);
        // 默认权重都是1，暂时只支持手动去Redis中修改权重
        apiKeys.forEach(apiKey -> map.put(apiKey, 1));

        // 删除多余的apikey
        map.readAllKeySet().forEach(apiKey -> {
            if (!apiKeys.contains(apiKey)) {
                map.remove(apiKey);
            }
        });
    }
}
