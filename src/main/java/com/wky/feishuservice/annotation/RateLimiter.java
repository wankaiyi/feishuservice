package com.wky.feishuservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wky
 * @date 2024/11/13
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {
    /**
     * 限制次数，默认10次
     */
    int limit() default 10;

    /**
     * 时间窗口大小，单位为秒，默认5分钟
     */
    int window() default 300;
}
