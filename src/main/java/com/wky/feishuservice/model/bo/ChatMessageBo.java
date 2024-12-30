package com.wky.feishuservice.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author 86159
 * @date 2024-12-30 23:07:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ChatMessageBo {
    private String text;
    private String messageId;
}
