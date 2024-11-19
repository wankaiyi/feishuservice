package com.wky.feishuservice.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * openai配置
 *
 * @author wky
 * @date 2024/11/11
 */
@Configuration
@Data
public class OpenaiConfig {

    @Value("${chatgpt.api-key}")
    private List<String> apiKeys;
}
