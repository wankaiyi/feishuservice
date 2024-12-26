package com.wky.feishuservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author wky
 * @date 2024/12/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FeishuCallbackResponseDTO {

    private String challenge;
    private Toast toast;

    /**
     * 客户端的弹窗提示
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Toast {

        /**
         * 弹窗类型
         * <p>
         * {@link ToastType}
         */
        private String type;
        private String content;

        @Getter
        @AllArgsConstructor
        public enum ToastType {
            INFO("info"),
            SUCCESS("success"),
            ERROR("error"),
            WARNING("warning");

            private final String value;
        }

    }

}
