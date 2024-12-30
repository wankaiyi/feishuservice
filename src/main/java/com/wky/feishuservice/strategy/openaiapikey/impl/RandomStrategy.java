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
 * 随机策略，随机选择一个apikey，如果没有配置就默认使用random
 *
 * @author wky
 * @date 2024/11/18
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "openai.apikey.strategy",
        havingValue = "random",
        matchIfMissing = true
)
public class RandomStrategy implements ApiKeySelectionStrategy {

    private final RedissonClient redissonClient;
    private final OpenAiConfig openaiConfig;
    private static final String API_KEY_LIST_KEY = "openai:random_strategy:api_key_list";
    private static final String LUA_SCRIPT = """
            local listKey = KEYS[1]
            local listLength = redis.call('LLEN', listKey)
            if listLength == 0 then return nil end
            local randomIndex = math.random(0, listLength - 1)
            return redis.call('LINDEX', listKey, randomIndex)
            """;

    @Override
    public String selectApiKey() {
        return redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        LUA_SCRIPT,
                        // 返回字符串类型
                        RScript.ReturnType.VALUE,
                        List.of(API_KEY_LIST_KEY));
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
    }
}
