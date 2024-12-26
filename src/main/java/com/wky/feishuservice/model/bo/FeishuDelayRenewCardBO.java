package com.wky.feishuservice.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author wky
 * @date 2024/12/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class FeishuDelayRenewCardBO {

    private String body;

    private String accessToken;
}
