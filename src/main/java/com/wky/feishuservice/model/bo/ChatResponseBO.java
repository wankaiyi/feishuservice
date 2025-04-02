package com.wky.feishuservice.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * @author wky
 * @date 2024/11/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ChatResponseBO {

    private String content;
    private String model;
    private Integer totalTokens;
    private BigDecimal price;
    private Double executeTime;

}
