package com.wky.feishuservice.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author wky
 * @date 2024/12/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "user_prompt")
public class UserPromptDO {

    private Long id;
    private String openId;
    private Long promptId;

}
