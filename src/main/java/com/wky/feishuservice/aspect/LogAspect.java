package com.wky.feishuservice.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wky.feishuservice.annotation.Log;
import com.wky.feishuservice.model.SystemLog;
import com.wky.feishuservice.utils.IPUtils;
import com.wky.feishuservice.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.MDC;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;


/**
 * @author wankaiyi
 */
@Aspect
@Component
@Slf4j
@Order(1)
@RequiredArgsConstructor
public class LogAspect {

    /**
     * 计算操作消耗时间
     */
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<>("Cost Time");
    private static final ObjectMapper objMapper = new ObjectMapper();

    /**
     * 处理请求前执行
     */
    @Before(value = "@annotation(com.wky.feishuservice.annotation.Log)")
    public void doBefore() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        MDC.put("service_name", "feishuservice");
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(log)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log log, Object jsonResult) {
        handleLog(joinPoint, log, null, jsonResult);
    }

    /**
     * 拦截异常操作
     *
     * @param joinPoint 切点
     * @param e         异常
     */
    @AfterThrowing(value = "@annotation(log)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log log, Exception e) {
        handleLog(joinPoint, log, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            HttpServletResponse response = ServletUtils.getResponse();
            SystemLog systemLog = new SystemLog();
            Long costTime = System.currentTimeMillis() - TIME_THREADLOCAL.get();
            String url = request.getRequestURI();
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            String ip = IPUtils.getIpAddr(request);
            systemLog.setClassMethod(className + "." + methodName + "()");
            systemLog.setStatus(response.getStatus());
            systemLog.setIp(ip);
            systemLog.setResponseTime(costTime);
            systemLog.setRequestMethod(request.getMethod());
            systemLog.setUrl(url);
            if (ObjectUtils.isNotEmpty(e)) {
                systemLog.setErrMsg(StringUtils.substring(e.getMessage(), 0, 1000));
            }
            if (controllerLog.isSaveRequestData()) {
                Object[] args = joinPoint.getArgs();
                // 过滤掉不可序列化的参数
                systemLog.setParams(Arrays.stream(args)
                        .map(arg -> {
                            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                                return arg.toString(); // 记录为字符串
                            } else {
                                try {
                                    return objMapper.writeValueAsString(arg);
                                } catch (Exception ex) {
                                    return arg.toString(); // 如果无法序列化，记录为字符串
                                }
                            }
                        }).toArray());
            }
            if (controllerLog.isSaveResponseData()) {
                systemLog.setResult(jsonResult);
            }
            String jsonStr = objMapper.writeValueAsString(systemLog);
            log.info(jsonStr);
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("异常信息:", exp);
        } finally {
            TIME_THREADLOCAL.remove();
        }
        MDC.remove("traceId");
    }

}
