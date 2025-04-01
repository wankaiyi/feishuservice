package com.wky.feishuservice.strategy.feishucardbutton.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wky.feishuservice.enumurations.FeishuCardButtonType;
import com.wky.feishuservice.mapper.UserPromptMapper;
import com.wky.feishuservice.mapper.UserPromptSubmissionsMapper;
import com.wky.feishuservice.model.dto.FeishuCallbackResponseDTO;
import com.wky.feishuservice.model.po.UserPromptDO;
import com.wky.feishuservice.model.po.UserPromptSubmissionsDO;
import com.wky.feishuservice.service.FeishuMessageService;
import com.wky.feishuservice.strategy.feishucardbutton.FeishuCardButtonStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author wky
 * @date 2024/12/29
 */
@Component
@RequiredArgsConstructor
public class PromptConfigSubmitStrategy implements FeishuCardButtonStrategy {

    private final UserPromptSubmissionsMapper userPromptSubmissionsMapper;
    private final UserPromptMapper userPromptMapper;
    private final FeishuMessageService feishuMessageService;

    @Override
    public void handle(String openMessageId, FeishuCallbackResponseDTO response, String token, String question) {
        String openId = UserInfoContext.getUserInfo().getReceiveId();
        UserPromptSubmissionsDO userPromptSubmissionsDO = userPromptSubmissionsMapper.selectOne(new LambdaQueryWrapper<UserPromptSubmissionsDO>()
                .eq(UserPromptSubmissionsDO::getMessageId, openMessageId));
        if (Objects.isNull(userPromptSubmissionsDO)) {
            response.setToast(new FeishuCallbackResponseDTO.Toast()
                    .setType(FeishuCallbackResponseDTO.Toast.ToastType.WARNING.getValue())
                    .setContent("表单不存在，请联系管理员处理"));
        } else {
            // 校验表单状态
            if (UserPromptSubmissionsDO.Status.SUBMITTED.getValue() == userPromptSubmissionsDO.getSubmitted()) {
                response.setToast(new FeishuCallbackResponseDTO.Toast()
                        .setType(FeishuCallbackResponseDTO.Toast.ToastType.WARNING.getValue())
                        .setContent("表单已提交，请勿重复提交"));
                return;
            }
            userPromptSubmissionsDO.setSubmitted(UserPromptSubmissionsDO.Status.SUBMITTED.getValue());
            userPromptSubmissionsMapper.updateById(userPromptSubmissionsDO);
            UserPromptDO userPromptDO = userPromptMapper.selectOne(new LambdaQueryWrapper<UserPromptDO>()
                    .eq(UserPromptDO::getOpenId, openId));
            if (Objects.isNull(userPromptDO)) {
                userPromptMapper.insert(new UserPromptDO()
                        .setOpenId(openId)
                        .setPromptId(userPromptSubmissionsDO.getPromptId()));
            } else {
                userPromptDO.setPromptId(userPromptSubmissionsDO.getPromptId());
		userPromptMapper.update(new LambdaUpdateWrapper<UserPromptDO>()
                        .eq(UserPromptDO::getId, userPromptDO.getId())
                        .set(UserPromptDO::getPromptId, userPromptSubmissionsDO.getPromptId()));
            }
            response.setToast(new FeishuCallbackResponseDTO.Toast()
                    .setType(FeishuCallbackResponseDTO.Toast.ToastType.SUCCESS.getValue())
                    .setContent("提交成功！"));
            feishuMessageService.delayRenewCardAsync(userPromptSubmissionsDO.getPromptId(), token);
        }
    }

    @Override
    public boolean isMatch(FeishuCardButtonType type) {
        return FeishuCardButtonType.PROMPT_CONFIG_SUBMIT.equals(type);
    }
}
