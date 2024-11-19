package com.wky.feishuservice.controller;

import com.alibaba.fastjson2.JSONObject;
import com.wky.feishuservice.annotation.Log;
import com.wky.feishuservice.annotation.RateLimiter;
import com.wky.feishuservice.service.FeishuMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wky
 * @date 2024/11/11
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/p2p")
@Slf4j
public class FeishuP2pController {

    private final FeishuMessageService feishuMessageService;

    @Log
    @RateLimiter
    @PostMapping("/chat")
    public JSONObject postFeishuNotice(@RequestBody JSONObject body) {
        return feishuMessageService.processFeishuNotice(body);
    }

}
