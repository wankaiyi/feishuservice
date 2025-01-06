package com.wky.feishuservice.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.wky.feishuservice.config.FeishuConfig;
import com.wky.feishuservice.model.dto.FeishuAlertDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * @author wky
 * @date 2024/11/21
 */
@Slf4j
public class AlertUtils {

    private static final FeishuConfig feishuConfig = SpringUtil.getBean(FeishuConfig.class);

    private static final HashMap<String, String> HEADERS_PARAMS = new HashMap<>() {{
        put("Content-Type", "application/json; charset=utf-8");
    }};
    private static final String ALERT_TEMPLATE = """
            {
                "msg_type": "text",
                "content": {
                    "text": %s
                }
            }
            """;

    /**
     * 请求未知异常告警
     */
    public static void sendUnknownErrorAlert(String uri, String errorMsg) {
        String feishuAlertWebHookUrl = feishuConfig.getFeishuAlertWebHookUrl();
        String body = String.format(ALERT_TEMPLATE, String.format("接口%s发生异常：%s", uri, errorMsg));
        String result = HttpUtils.postForm(feishuAlertWebHookUrl, body, HEADERS_PARAMS);
        log.info("接口请求未知异常告警，url：{}，请求体：{}， 响应：{}", feishuAlertWebHookUrl, body, result);
    }

    /**
     * 异常告警
     */
    public static String sendErrorAlert(String errorMsg, String alertUrl) {
        String body = String.format(ALERT_TEMPLATE, JacksonUtils.serialize(errorMsg));
        String webhookUrl = StringUtils.isBlank(alertUrl)
                ? feishuConfig.getFeishuAlertWebHookUrl()
                : alertUrl;
        String result = HttpUtils.postForm(webhookUrl, body, HEADERS_PARAMS);
        log.info("服务异常告警，url：{}，请求体：{}， 响应：{}", webhookUrl, body, result);
        return result;
    }

    public static String sendErrorAlert(FeishuAlertDTO feishuAlertDTO) {
        String token = feishuAlertDTO.getToken();
        if (!StringUtils.equals(token, feishuConfig.getFeishuAlertToken())) {
            return "身份认证失败";
        }
        return sendErrorAlert(feishuAlertDTO.getMessage(), feishuAlertDTO.getAlertUrl());
    }

}
