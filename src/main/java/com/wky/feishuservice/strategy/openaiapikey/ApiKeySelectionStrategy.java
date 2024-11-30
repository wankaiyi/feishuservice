package com.wky.feishuservice.strategy.openaiapikey;

/**
 * api key选择策略接口
 *
 * @author wky
 * @date 2024/11/11
 */
public interface ApiKeySelectionStrategy {

    String selectApiKey();

    void init() throws IllegalAccessException;
}
