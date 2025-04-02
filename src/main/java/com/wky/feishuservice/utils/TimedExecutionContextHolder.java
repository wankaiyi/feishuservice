package com.wky.feishuservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NamedThreadLocal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * @author wankaiyi
 * @date 2025/04/02
 */
@Slf4j
public class TimedExecutionContextHolder {

    private static final ThreadLocal<Deque<Long>> LOOKUP_KEY_HOLDER = new NamedThreadLocal<>("start-time") {
        @Override
        protected Deque<Long> initialValue() {
            return new ArrayDeque<>();
        }
    };

    private TimedExecutionContextHolder() {
    }

    public static Long peek() {
        return LOOKUP_KEY_HOLDER.get().peek();
    }

    public static Long push(Long startTime) {
        Long start = Objects.isNull(startTime) ? System.currentTimeMillis() : startTime;
        LOOKUP_KEY_HOLDER.get().push(start);
        return start;
    }

    public static Long poll() {
        Deque<Long> deque = LOOKUP_KEY_HOLDER.get();
        if (deque.isEmpty()) {
            LOOKUP_KEY_HOLDER.remove();
        }
        return deque.poll();
    }

    public static void clear() {
        LOOKUP_KEY_HOLDER.remove();
    }

    public static double getExecuteTime() {
        Long startTime = peek();
        if (Objects.isNull(startTime)) {
            String callerMethodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            log.error("调用状态不合法，调用方法：{}", callerMethodName);
            return 0;
        }
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        return executionTime / 1e3;
    }
}

