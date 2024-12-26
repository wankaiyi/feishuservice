package com.wky.feishuservice.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    @TableId(type = IdType.AUTO)
    private Long id;
    private String act;
    private String prompt;
}
