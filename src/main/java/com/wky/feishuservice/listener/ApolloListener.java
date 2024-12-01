package com.wky.feishuservice.listener;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.wky.feishuservice.client.OpenAiClient;
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
    private final OpenAiClient openAiClient;

    private static final String CHATGPT_API_KEYS = "chatgpt.api-keys";

    /**
     * 自定义的apollo配置监听器和apollo Java客户端自带的配置监听器是不同线程执行的，
     * 自定义的所有配置监听器也是不同线程执行的，所以在这个方法开始时和结束时分别获取一
     * 次配置得到的结果可能不同
     */
    @ApolloConfigChangeListener(value = {"openai"})
    public void onChange(ConfigChangeEvent changeEvent) {
        for (String changedKey : changeEvent.changedKeys()) {
            if (CHATGPT_API_KEYS.equals(changedKey)) {
                log.info("监听到apollo配置变化：{}", openAiConfig.getApiKeys());
                try {
                    // 延迟1秒，等待实际配置更新
                    Thread.sleep(1000);
                    apiKeySelectionStrategy.init();
                    openAiClient.init();
                } catch (IllegalAccessException e) {
                    log.error("apikeys为空，无法更新配置");
                    String oldValue = changeEvent.getChange(changedKey).getOldValue();
                    openAiConfig.setApiKeys(List.of(oldValue.split(",")));
                } catch (InterruptedException e) {

                }
            }
        }
        log.info("更新配置完成：{}", openAiConfig.getApiKeys());
    }
}
