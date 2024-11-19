package com.wky.feishuservice.utils;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author wky
 * @date 2024/11/11
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final RedissonClient redissonClient;

    public void set(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.setAsync(value);
    }

    public void set(String key, String value, long expireTime, TimeUnit timeUnit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.setAsync(value, expireTime, timeUnit);
    }

    // 获取对应于 Redis 的 String 类型的值
    public String get(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public void expire(String key, long expireTime, TimeUnit timeUnit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.expireAsync(Duration.ofMillis(timeUnit.toMillis(expireTime)));
    }

}
