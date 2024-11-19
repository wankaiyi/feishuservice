package com.wky.feishuservice.strategy.openaiapikey.impl;

import com.wky.feishuservice.config.OpenaiConfig;
import com.wky.feishuservice.strategy.openaiapikey.ApiKeySelectionStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 最少使用策略
 *
 * @author wky
 * @date 2024/11/11
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "openai.apikey.strategy",
        havingValue = "least-usage"
)
public class LeastUsageStrategy implements ApiKeySelectionStrategy {
    private final RedissonClient redissonClient;
    private final OpenaiConfig openaiConfig;
    private static final String API_KEY_USAGE_COUNT_ZSET = "openai:least_usage_strategy:api_key_set";
    private static final String LUA_SCRIPT = """
                local zsetKey = KEYS[1]
                local keyWithMinScore = redis.call('ZRANGE', zsetKey, 0, 0, 'WITHSCORES')
                if keyWithMinScore == nil or #keyWithMinScore == 0 then
                    return nil
                end
                local member = keyWithMinScore[1]
                redis.call('ZINCRBY', zsetKey, 1, member)
                return member
                """;

    @Override
    public String selectApiKey() {
        return redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        LUA_SCRIPT,
                        RScript.ReturnType.STATUS,
                        List.of(API_KEY_USAGE_COUNT_ZSET));
    }

    @PostConstruct
    public void init() throws IllegalAccessException {
        List<String> apiKeys = openaiConfig.getApiKeys();
        if (CollectionUtils.isEmpty(apiKeys)) {
            throw new IllegalAccessException("apikeys不能为空");
        }
        RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(API_KEY_USAGE_COUNT_ZSET);
        // 添加zset中没有的apikey
        apiKeys.forEach(apiKey -> {
            if (!sortedSet.contains(apiKey)) {
                sortedSet.addAsync(0 ,apiKey);
            }
        });
        // 删除多余的apikey
        sortedSet.forEach(apiKey -> {
            if (!apiKeys.contains(apiKey)) {
                sortedSet.removeAsync(apiKey);
            }
        });
    }
}
