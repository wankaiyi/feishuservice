package com.wky.feishuservice.exceptions;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.utils.SpringContextUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wankaiyi
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class FeishuP2pException extends RuntimeException {

    private String message;

    private String receiveId;

    private Map<String, String> headerParams;

    private Map<String, String> requestParams;

    public FeishuP2pException(String message, String receiveId, String receiveIdType) {
        this.message = message;
        this.receiveId = receiveId;
        this.headerParams = new HashMap<>() {{
            put("Content-Type", "application/json; charset=utf-8");
            FeishuClient feishuClient = SpringContextUtil.getBean(FeishuClient.class);
            put("Authorization", "Bearer " + feishuClient.getTenantAccessToken());
        }};
        this.requestParams = new HashMap<>() {{
            put("receive_id_type", receiveIdType);
        }};
    }
}
