package com.wky.feishuservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequestDTO {
    private String model;
    private List<Message> messages;
    private Prediction prediction;
    private Boolean stream;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Prediction {
        private String type = "content";
        private String content;
    }
}
