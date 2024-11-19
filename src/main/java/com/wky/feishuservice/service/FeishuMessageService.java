package com.wky.feishuservice.service;

import com.alibaba.fastjson2.JSONObject;

/**
 * @author wky
 * @date 2024/11/11
 */
public interface FeishuMessageService {
    JSONObject processFeishuNotice(JSONObject body);
}
