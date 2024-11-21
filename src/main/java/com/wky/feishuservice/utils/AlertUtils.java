package com.wky.feishuservice.utils;

import java.util.HashMap;

/**
 * @author wky
 * @date 2024/11/21
 */
public class AlertUtils {

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

    /**
     * 请求未知异常告警
     */
    public static void sendUnknownErrorAlert(String uri, String errorMsg) {
        HttpUtils.postForm(ALERT_WEBHOOK_URL, String.format(ALERT_TEMPLATE, String.format("接口%s发生异常：%s", uri, errorMsg)), HEADERS_PARAMS);
    }

    /**
     * 线程池触发拒绝策略告警
     */
    public static void sendThreadPoolRejectAlert(String errorMsg) {
        HttpUtils.postForm(ALERT_WEBHOOK_URL, String.format(ALERT_TEMPLATE, errorMsg), HEADERS_PARAMS);
    }

}
