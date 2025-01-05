package com.wky.feishuservice.handler;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.utils.AlertUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * @author wky
 * @date 2024/10/12
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @Value("${env}")
    private String env;

    @Resource(name = "feishuWebhookPoolTaskExecutor")
    private ThreadPoolTaskExecutor feishuWebhookPoolTaskExecutor;

    private final FeishuClient feishuClient;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e, HttpServletRequest request) {
        if (StringUtils.equals(env, "dev")) {
            return ResponseEntity.ok().body(e.getMessage());
        }
        CompletableFuture.runAsync(
                () -> AlertUtils.sendUnknownErrorAlert(request.getRequestURI(), e.getMessage() + Arrays.toString(e.getStackTrace())),
                feishuWebhookPoolTaskExecutor);
        return ResponseEntity.ok().body(e.getMessage());
    }

    @ExceptionHandler(FeishuP2pException.class)
    public void handleFeishuP2pException(FeishuP2pException e) {
        feishuClient.handleP2pException(e);
    }

}
