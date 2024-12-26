package com.wky.feishuservice.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wky
 * @date 2024/12/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "prompts")
public class PromptDO {
    private Long id;
    private String act;
    private String prompt;
}
