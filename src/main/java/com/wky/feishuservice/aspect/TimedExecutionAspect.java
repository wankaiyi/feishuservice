package com.wky.feishuservice.aspect;

import com.wky.feishuservice.annotation.TimedExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.LinkedList;

/**
 * @author wky
 * @date 2024/11/12
 */
@Component
@Aspect
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class TimedExecutionAspect {

    ThreadLocal<Deque<Long>> startTimes = ThreadLocal.withInitial(LinkedList::new);

    @Before("@annotation(timedExecution)")
    public void doBefore(TimedExecution timedExecution) {
        startTimes.get().push(System.currentTimeMillis());
        log.info("\"{}\"方法开始执行", timedExecution.methodDescription());
    }

    @AfterReturning("@annotation(timedExecution)")
    public void doAfter(TimedExecution timedExecution) {
        handleAfter(timedExecution.methodDescription(), null);
    }

    @AfterThrowing(pointcut = "@annotation(timedExecution)", throwing = "e")
    public void doAfterThrowing(TimedExecution timedExecution, Throwable e) {
        handleAfter(timedExecution.methodDescription(), e);
    }

    public void handleAfter(String methodDescription, Throwable e) {
        long startTime = startTimes.get().pop();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        if (e == null) {
            log.info("\"{}\"方法执行结束，执行时间：{}ms", methodDescription, executionTime);
        } else {
            // 有异常记录异常信息，不将异常重新抛出去，防止记录的错误日志信息过多不方便定位
            log.error("\"{}\"方法执行结束，执行时间：{}ms，出现异常：", methodDescription, executionTime, e);
        }
    }
}
