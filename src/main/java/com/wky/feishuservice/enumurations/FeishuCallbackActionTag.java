package com.wky.feishuservice.enumurations;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wky
 * @date 2024/12/25
 */
@Getter
@AllArgsConstructor
public enum FeishuCallbackActionTag {

    SELECT_STATIC("select_static"),
    BUTTON("button");

    private final String value;


}
