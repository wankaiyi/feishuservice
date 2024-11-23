package com.wky.feishuservice.utils;

import com.wky.feishuservice.model.common.UserInfo;

/**
 * @author wky
 * @date 2024/11/23
 */
public class UserInfoContext {

    private static final ThreadLocal<UserInfo> threadLocal = new ThreadLocal<>();

    public static void setUserInfo(UserInfo userInfo) {
        threadLocal.set(userInfo);
    }

    public static UserInfo getUserInfo() {
        return threadLocal.get();
    }

    public static void removeUserInfo() {
        threadLocal.remove();
    }
}
