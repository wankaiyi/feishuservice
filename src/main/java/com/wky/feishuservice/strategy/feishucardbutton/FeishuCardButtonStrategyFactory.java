package com.wky.feishuservice.strategy.feishucardbutton;

import cn.hutool.extra.spring.SpringUtil;
import com.wky.feishuservice.enumurations.FeishuCardButtonType;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wky
 * @date 2024/12/29
 */
public class FeishuCardButtonStrategyFactory {

    private FeishuCardButtonStrategyFactory() {
    }

    public static FeishuCardButtonStrategy getStrategy(FeishuCardButtonType type) {
        Map<String, FeishuCardButtonStrategy> feishuMessageStrategyList = SpringUtil.getBeansOfType(FeishuCardButtonStrategy.class);
        AtomicReference<FeishuCardButtonStrategy> strategy = new AtomicReference<>();
        feishuMessageStrategyList.forEach((key, value) -> {
            if (value.isMatch(type)) {
                strategy.set(value);
            }
        });
        return strategy.get();
    }
}
