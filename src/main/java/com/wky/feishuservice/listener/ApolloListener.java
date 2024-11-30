package com.wky.feishuservice.listener;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.wky.feishuservice.config.OpenAiConfig;
import com.wky.feishuservice.strategy.openaiapikey.ApiKeySelectionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author wky
 * @date 2024/11/30
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApolloListener {

    private final ApiKeySelectionStrategy apiKeySelectionStrategy;
    private final OpenAiConfig openAiConfig;

    private static final String CHATGPT_API_KEYS = "chatgpt.api-keys";

    @ApolloConfigChangeListener
    public void onChange(ConfigChangeEvent changeEvent) {
        for (String changedKey : changeEvent.changedKeys()) {
            if (CHATGPT_API_KEYS.equals(changedKey)) {
                try {
                    apiKeySelectionStrategy.init();
                } catch (IllegalAccessException e) {
                    log.error("apikeys为空，无法更新配置");
                    String oldValue = changeEvent.getChange(changedKey).getOldValue();
                    openAiConfig.setApiKeys(List.of(oldValue.split(",")));
                }
            }
        }
    }
}
