package com.wky.feishuservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author wky
 * @date 2024/11/22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageGenerateRequestDTO {

    /**
     * 用于图像生成的模型。
     */
    private String model;

    /**
     * 要生成的图像数。必须介于 1 和 10 之间。
     */
    private Long n = 1L;

    /**
     * 所需图像的文本描述。最大长度为 1000 个字符。
     */
    private String prompt;

    /**
     * 将生成的图像的质量。`hd`创建具有更精细细节和更高一致性的图像。此参数仅支持`dall-e-3`.
     */
    private String quality;

    /**
     * 返回生成的图像的格式。必须是 b64_json或url之一。由于网络原因，我们选择base64
     */
    private String responseFormat = "b64_json";

    /**
     * 生成图像的大小。必须是256x256、512x512或 1024x1024之一。
     */
    private String size;

    /**
     * 生成图像的大小。必须是`256x256`、`512x512`或`1024x1024`for之一`dall-e-2`。对于模型来说，必须是`1024x1024`、`1792x1024`、
     * 或之一。`1024x1792``dall-e-3`
     */
    private String style;

    /**
     * 生成图像的风格。必须是 或`vivid`之一`natural`。生动使模型倾向于生成超真实和戏剧性的图像。自然使模型生成更自然、不太真实的图像。此参数仅支持`dall-e-3`.
     */
    private String user;
}
