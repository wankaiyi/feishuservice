package com.wky.feishuservice.exceptions;

/**
 * @author wky
 * @date 2024/11/18
 */
public class OpenAiException extends RuntimeException {

    public OpenAiException(String message) {
        super(message);
    }

}
