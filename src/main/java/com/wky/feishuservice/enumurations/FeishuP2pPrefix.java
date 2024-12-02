package com.wky.feishuservice.enumurations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author wky
 * @date 2024/11/25
 */
@Getter
@AllArgsConstructor
public enum FeishuP2pPrefix {

    IMAGE_GENERATE("#image"),
    REFRESH_MEMORY("#reset"),
    WEATHER("#天气"),
    THREAD_POOL_INFO("#tp");

    private final String prefix;

    public static List<FeishuP2pPrefix> getFeishuP2pPrefixes() {
        return List.of(values());
    }

    public static boolean notInFeishuP2pPrefixes(String contentText) {
        return getFeishuP2pPrefixes().stream().noneMatch(prefix -> StringUtils.startsWith(contentText, prefix.getPrefix()));
    }

}
