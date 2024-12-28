package com.wky.feishuservice.constants;

/**
 * @author wky
 * @date 2024/11/11
 */
public class FeishuConstants {

    public static final String FEISHU_OPENAI_APP_ID = "cli_a7bbd1060d10500d";

    public static final String FEISHU_OPENAI_APP_SECRET = "TEfOAwPNcENO1DWiE36PMcFxhFNbdsB8";

    public static final String FEISHU_OPENAI_REDIS_KEY = "feishu:openai:tenantAccessToken";

    public static final String FEISHU_SEND_MESSAGE_TO_USER_URL = "https://open.feishu.cn/open-apis/im/v1/messages";

    public static final String FEISHU_REPLY_USER_URL = "https://open.feishu.cn/open-apis/im/v1/messages/%s/reply";

    public static final String FEISHU_GET_TENANT_ACCESS_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";

    public static final String FEISHU_UPLOAD_IMAGE_URL = "https://open.feishu.cn/open-apis/im/v1/images";

    public static final String FEISHU_EVENT_KEY = "feishu:eventId:%s";

    public static final String FEISHU_DELAY_UPDATE_CARD_URL = "https://open.feishu.cn/open-apis/interactive/v1/card/update";

    public static final String FEISHU_PROMPT_OPTION_LOCK_KEY = "feishu:prompt:option:lock:%s";

    public static String getFeishuEventKey(String eventId) {
        return String.format(FEISHU_EVENT_KEY, eventId);
    }

    public static String getFeishuPromptOptionLockKey(String userId) {
        return String.format(FEISHU_PROMPT_OPTION_LOCK_KEY, userId);
    }
}
