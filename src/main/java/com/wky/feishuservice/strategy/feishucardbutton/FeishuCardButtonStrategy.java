package com.wky.feishuservice.strategy.feishucardbutton;

import com.wky.feishuservice.enumurations.FeishuCardButtonType;
import com.wky.feishuservice.model.dto.FeishuCallbackResponseDTO;

/**
 * @author wky
 * @date 2024/12/29
 */
public interface FeishuCardButtonStrategy {

    void handle(String openMessageId, FeishuCallbackResponseDTO response, String token, String question);

    boolean isMatch(FeishuCardButtonType type);
}
