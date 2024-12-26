package com.wky.feishuservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wky.feishuservice.model.po.UserPromptDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author wky
 * @date 2024/12/25
 */
@Mapper
public interface UserPromptMapper extends BaseMapper<UserPromptDO> {
    String selectUserPrompt(String openId);
}
