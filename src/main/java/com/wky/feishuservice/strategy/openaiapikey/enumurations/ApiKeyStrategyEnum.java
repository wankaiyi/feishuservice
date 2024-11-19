package com.wky.feishuservice.strategy.openaiapikey.enumurations;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wky
 * @date 2024/11/18
 */
@Getter
@AllArgsConstructor
public enum ApiKeyStrategyEnum {

    RANDOM("random"),
    LEAST_USAGE("least-usage"),
    ROUND_ROBIN("round-robin"),
    WEIGHTED_ROUND_ROBIN("weighted-round-robin");

    private final String strategyName;

    public static List<String> getApiKeyStrategies() {
        return Arrays.stream(ApiKeyStrategyEnum.values()).map(ApiKeyStrategyEnum::getStrategyName).collect(Collectors.toList());
    }
}
