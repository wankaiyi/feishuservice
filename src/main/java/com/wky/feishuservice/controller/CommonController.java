package com.wky.feishuservice.controller;

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

    @GetMapping("/version")
    public String getVersion() {
        return "1.1.0";
    }
}
