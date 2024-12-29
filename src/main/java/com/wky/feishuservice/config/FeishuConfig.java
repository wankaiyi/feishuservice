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

    @Value("${feishu.openai.app.id:cli_a7bbd1060d10500d}")
    private String feishuOpenaiAppId;

    @Value("${feishu.openai.app.secret:TEfOAwPNcENO1DWiE36PMcFxhFNbdsB8}")
    private String feishuOpenaiAppSecret;

}
