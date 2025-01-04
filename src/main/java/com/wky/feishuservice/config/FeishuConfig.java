package com.wky.feishuservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author wky
 * @date 2024/11/11
 */
@Configuration
@Getter
public class FeishuConfig {

    @Value("${feishu.openai.app.id}")
    private String feishuOpenaiAppId;

    @Value("${feishu.openai.app.secret}")
    private String feishuOpenaiAppSecret;

    /**
     * 飞书告警webhook地址
     */
    @Value("${feishu.alert.webhook.url}")
    private String feishuAlertWebHookUrl;

}
