package com.wky.feishuservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书发送消息响应体
 *
 * @author wky
 * @date 2024/11/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeishuP2pResponseDTO {
    private int code;
    private String msg;
    private Data data;
    private Error error;

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Error {
        private String logId;
        private String troubleshooter;
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Data {
        private String messageId;
        private String rootId;
        private String parentId;
        private String threadId;
        private String msgType;
        private String createTime;
        private String updateTime;
        private boolean deleted;
        private boolean updated;
        private String chatId;
        private Sender sender;
        private Body body;
        private Mention[] mentions;
        private String upperMessageId;

        @lombok.Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Sender {
            private String id;
            private String idType;
            private String senderType;
            private String tenantKey;
        }

        @lombok.Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Body {
            private String content;
        }

        @lombok.Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Mention {
            private String key;
            private String id;
            private String idType;
            private String name;
            private String tenantKey;
        }
    }
}
