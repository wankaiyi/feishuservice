package com.wky.feishuservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wky
 * @date 2024/11/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponseDTO {
    private String id;
    private String object;
    private long created;
    private String model;
    private Choice[] choices;
    private Usage usage;
    private String systemFingerprint;
    private ChatError error;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Choice {
        private int index;
        @JsonProperty("message")
        private MessageResp messageResp;
        private Object logprobs;
        private String finishReason;


        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class MessageResp {
            private String role;
            private String content;
            private Object refusal;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        private PromptTokensDetails promptTokensDetails;
        private CompletionTokensDetails completionTokensDetails;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class PromptTokensDetails {
            private int cachedTokens;
            private int audioTokens;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class CompletionTokensDetails {
            private int reasoningTokens;
            private int audioTokens;
            private int acceptedPredictionTokens;
            private int rejectedPredictionTokens;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ChatError {
        private String message;
        private String type;
        private String param;
        private String code;
    }
}
