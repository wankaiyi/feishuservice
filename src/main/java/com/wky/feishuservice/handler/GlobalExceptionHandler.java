package com.wky.feishuservice.handler;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.utils.AlertUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

/**
 * @author wky
 * @date 2024/10/12
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final FeishuClient feishuClient;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e, HttpServletRequest request) {
        AlertUtils.sendUnknownErrorAlert(request.getRequestURI(), e.getMessage() + Arrays.toString(e.getStackTrace()));
        return ResponseEntity.ok().body(e.getMessage());
    }

    @ExceptionHandler(FeishuP2pException.class)
    public void handleFeishuP2pException(FeishuP2pException e) {
        feishuClient.handleP2pException(e);
    }

}
