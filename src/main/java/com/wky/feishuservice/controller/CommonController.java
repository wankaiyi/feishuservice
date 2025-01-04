package com.wky.feishuservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wky
 * @date 2024/12/31
 */
@RestController
@RequestMapping("/common")
public class CommonController {
    /**
     * 版本号
     */
    @Value("${version:1.1.0}")
    private String version;

    @GetMapping("/version")
    public String getVersion() {
        return version;
    }
}
