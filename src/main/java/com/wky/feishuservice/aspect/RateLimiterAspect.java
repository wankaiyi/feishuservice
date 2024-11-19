package com.wky.feishuservice.aspect;

import com.wky.feishuservice.annotation.RateLimiter;
import com.wky.feishuservice.utils.IPUtils;
import com.wky.feishuservice.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author wky
 * @date 2024/11/13
 */
@Component
@Aspect
@RequiredArgsConstructor
@Slf4j
@Order(0)
public class RateLimiterAspect {

    private final RedissonClient redissonClient;

    @Before("@annotation(rateLimiter)")
    public void rateLimiter(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = joinPoint.getSignature().getName();
        int limit = rateLimiter.limit();
        int window = rateLimiter.window();
        HttpServletRequest request = ServletUtils.getRequest();
        String ip = IPUtils.getIpAddr(request);
        if (!isAllowed(ip, limit, window, methodName)) {
            log.error("访问过于频繁，ip：{} 被限流", ip);
            throw new RuntimeException(String.format("%s方法访问过于频繁，%s秒内访问次数超过%s次，ip：%s 被限流", methodName, window, limit, ip));
        }
    }

    public boolean isAllowed(String ip, int limit, int window, String methodName) {
        // 生成基于IP的限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("feishu:" + methodName + ":ipRateLimiter:" + ip);

        rateLimiter.trySetRate(RateType.OVERALL, limit, window, RateIntervalUnit.SECONDS);

        // 尝试获取一个令牌
        return rateLimiter.tryAcquire(1);
    }
}
