package com.wky.feishuservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author wky
 * @date 2024/11/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDTO {
    private String model;
    private List<Message> messages;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Message {
        private String role;
        private String content;
    }
}
