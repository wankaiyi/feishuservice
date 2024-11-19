package com.wky.feishuservice.enumurations;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wky
 * @date 2024/11/11
 */
@Getter
@AllArgsConstructor
public enum ReceiveType {

    OPEN_ID("open_id"),
    USER_ID("user_id"),
    CHAT_ID("chat_id"),
    UNION_ID("union_id"),
    EMAIL("email");

    private final String value;


}
