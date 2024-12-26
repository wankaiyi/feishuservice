package com.wky.feishuservice.model.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author wky
 * @date 2024/12/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "user_prompt_submissions")
public class UserPromptSubmissionsDO {
    private Long id;
    private String messageId;
    private String openId;
    private Long promptId;
    /**
     * 0:未提交，1:已提交
     * <p>
     * {@link Status}
     */
    @TableField(value = "is_submitted")
    private Short submitted;

    @Getter
    @AllArgsConstructor
    public enum Status {
        UN_SUBMITTED((short) 0),
        SUBMITTED((short) 1);

        private final short value;
    }
}
