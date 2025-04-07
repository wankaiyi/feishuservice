package com.wky.feishuservice.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wky
 * @date 2024/10/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "lc_cache")
public class LeetCodeCacheDO {
    private Integer id;
    private String title;
    private String content;
}
