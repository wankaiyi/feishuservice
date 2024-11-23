package com.wky.feishuservice.model.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class SystemLog implements Serializable {

    private String url;

    private String classMethod;

    private String requestMethod;

    private String ip;

    private Long responseTime;

    private Object[] params;

    private Object result;

    private Integer status;

    private String errMsg;

}
