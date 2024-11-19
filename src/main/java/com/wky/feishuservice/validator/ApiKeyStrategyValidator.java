package com.wky.feishuservice.validator;

import com.wky.feishuservice.strategy.openaiapikey.enumurations.ApiKeyStrategyEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author wky
 * @date 2024/11/18
 */
@Component
@RequiredArgsConstructor
public class ApiKeyStrategyValidator {

    private final Environment environment;

    @PostConstruct
    public void validateStrategy() {
        String strategy = environment.getProperty("openai.apikey.strategy", "random");
        List<String> allowedApiKeyStrategies = ApiKeyStrategyEnum.getApiKeyStrategies();
        if (!allowedApiKeyStrategies.contains(strategy)) {
            throw new IllegalArgumentException(
                    String.format("Invalid openai.apikey.strategy value: '%s'. Allowed values are: %s",
                            strategy, allowedApiKeyStrategies)
            );
        }
    }
}

