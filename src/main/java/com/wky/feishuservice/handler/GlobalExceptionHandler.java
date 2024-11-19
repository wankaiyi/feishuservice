package com.wky.feishuservice.handler;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.HashMap;

/**
 * @author wky
 * @date 2024/10/12
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final FeishuClient feishuClient;

    public static final String ALERT_WEBHOOK_URL = "https://open.feishu.cn/open-apis/bot/v2/hook/72dca8cc-0b1d-438c-8012-b8d1fbfca131";
    private static final HashMap<String, String> HEADERS_PARAMS = new HashMap<>() {{
        put("Content-Type", "application/json; charset=utf-8");
    }};
    private static final String ALERT_TEMPLATE = """
            {
                "msg_type": "text",
                "content": {
                    "text": "%s"
                }
            }
            """;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e, HttpServletRequest request) {
        HttpUtils.postForm(ALERT_WEBHOOK_URL, String.format(ALERT_TEMPLATE, String.format("接口%s发生异常：%s", request.getRequestURI(), e.getMessage() + Arrays.toString(e.getStackTrace()))), HEADERS_PARAMS);
        return ResponseEntity.ok().body(e.getMessage());
    }

    @ExceptionHandler(FeishuP2pException.class)
    public void handleFeishuP2pException(FeishuP2pException e) {
        feishuClient.handelP2pException(e);
    }

}
