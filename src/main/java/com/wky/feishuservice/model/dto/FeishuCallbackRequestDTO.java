package com.wky.feishuservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.wky.feishuservice.enumurations.FeishuCardButtonType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author wky
 * @date 2024/12/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FeishuCallbackRequestDTO {

    private String schema;
    private Header header;
    private Event event;
    private String type;
    private String challenge;
    private String token;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Header {

        private String eventId;
        private String token;
        private String createTime;
        private String eventType;
        private String tenantKey;
        private String appId;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Event {

        private Operator operator;
        /**
         * 这个token是用于更新卡片的
         */
        private String token;
        private Action action;
        private String host;
        private Context context;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Operator {

            private String tenantKey;
            private String userId;
            private String openId;
            private String unionId;

        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Action {

            private String tag;
            private String option;
            private Map<String, FeishuCardButtonType> value;

        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Context {

            private String openMessageId;
            private String openChatId;

        }
    }

}
