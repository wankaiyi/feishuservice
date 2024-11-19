package com.wky.feishuservice.selector;

import com.wky.feishuservice.strategy.openaiapikey.ApiKeySelectionStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * api key上下文
 *
 * @author wky
 * @date 2024/11/11
 */
@Component
@RequiredArgsConstructor
@DependsOn("apiKeyStrategyValidator")
public class ApiKeySelector {

    private final ApiKeySelectionStrategy strategy;

    public String selectApiKey() {
        return strategy.selectApiKey();
    }
}
