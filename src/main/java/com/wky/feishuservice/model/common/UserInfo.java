package com.wky.feishuservice.model.common;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author wky
 * @date 2024/11/23
 */
@Data
@Accessors(chain = true)
public class UserInfo {

    private String receiveId;

    private String receiveType;


}
