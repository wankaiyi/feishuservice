package com.wky.feishuservice.controller;

import com.wky.feishuservice.utils.AlertUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wky
 * @date 2024/12/17
 */
@RestController
@RequestMapping("/alert")
public class FeishuAlertController {

    @PostMapping
    public String sendAlert(@RequestBody String message) {
        return AlertUtils.sendErrorAlert(message);
    }
}
