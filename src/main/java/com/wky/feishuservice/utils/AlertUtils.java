package com.wky.feishuservice.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.wky.feishuservice.config.FeishuConfig;

import java.util.HashMap;

/**
 * @author wky
 * @date 2024/11/21
 */
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
        HttpUtils.postForm(feishuConfig.getFeishuAlertWebHookUrl(), String.format(ALERT_TEMPLATE, String.format("接口%s发生异常：%s", uri, errorMsg)), HEADERS_PARAMS);
    }

    /**
     * 异常告警
     */
    public static String sendErrorAlert(String errorMsg) {
        return HttpUtils.postForm(feishuConfig.getFeishuAlertWebHookUrl(), String.format(ALERT_TEMPLATE, JacksonUtils.serialize(errorMsg)), HEADERS_PARAMS);

    }

}
