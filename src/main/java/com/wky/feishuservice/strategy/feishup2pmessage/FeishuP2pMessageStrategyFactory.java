package com.wky.feishuservice.strategy.feishup2pmessage;

import cn.hutool.extra.spring.SpringUtil;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wky
 * @date 2024/11/22
 */
public class FeishuP2pMessageStrategyFactory {

    private FeishuP2pMessageStrategyFactory() {
    }

    public static FeishuP2pMessageStrategy getStrategy(String contentText) {
        Map<String, FeishuP2pMessageStrategy> feishuMessageStrategyList = SpringUtil.getBeansOfType(FeishuP2pMessageStrategy.class);
        AtomicReference<FeishuP2pMessageStrategy> strategy = new AtomicReference<>();
        feishuMessageStrategyList.forEach((key, value) -> {
            if (value.isMatch(contentText)) {
                strategy.set(value);
            }
        });
        return strategy.get();
    }
}
