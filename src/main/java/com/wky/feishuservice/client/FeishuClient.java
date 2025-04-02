package com.wky.feishuservice.client;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wky.feishuservice.annotation.TimedExecution;
import com.wky.feishuservice.config.FeishuConfig;
import com.wky.feishuservice.constants.FeishuConstants;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.mapper.PromptMapper;
import com.wky.feishuservice.mapper.UserPromptSubmissionsMapper;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.bo.FeishuComboboxOptionBO;
import com.wky.feishuservice.model.bo.FeishuDelayRenewCardBO;
import com.wky.feishuservice.model.bo.WeatherInfoBO;
import com.wky.feishuservice.model.dto.FeishuP2pResponseDTO;
import com.wky.feishuservice.model.dto.FeishuSendUserMsgDTO;
import com.wky.feishuservice.model.dto.FeishuUploadResponseDTO;
import com.wky.feishuservice.model.po.PromptDO;
import com.wky.feishuservice.model.po.UserPromptSubmissionsDO;
import com.wky.feishuservice.producer.FeishuRenewCardProducer;
import com.wky.feishuservice.utils.HttpUtils;
import com.wky.feishuservice.utils.JacksonUtils;
import com.wky.feishuservice.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.wky.feishuservice.constants.FeishuConstants.FEISHU_OPENAI_REDIS_KEY;

/**
 * @author wky
 * @date 2024/11/11
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeishuClient {
    /**
     * tenant令牌过期时间
     */
    @Value("${feishu.tenant.token.expire.time:5400000}")
    private long tenantExpireTime;

    private final RedisUtils redisUtils;
    private final PromptMapper promptMapper;
    private final FeishuRenewCardProducer feishuRenewCardProducer;
    private final UserPromptSubmissionsMapper userPromptSubmissionsMapper;
    private final FeishuConfig feishuConfig;

    public FeishuP2pResponseDTO sendP2pMsg(ChatResponseBO chatResponseBO, String receiveId, String receiveIdType, String msgType, String messageId) {
        FeishuClient self = SpringUtil.getBean(FeishuClient.class);
        return self.sendFeishuP2pMsg(getContent(chatResponseBO), receiveId, receiveIdType, msgType, messageId);
    }

    @TimedExecution(methodDescription = "飞书发送点对点消息")
    public FeishuP2pResponseDTO sendFeishuP2pMsg(String content, String receiveId, String receiveIdType, String msgType, String messageId) {
        receiveIdType = StringUtils.isBlank(receiveIdType) ? "open_id" : receiveIdType;
        String accessToken = getTenantAccessToken();
        FeishuSendUserMsgDTO feishuSendUserMsgDTO = new FeishuSendUserMsgDTO()
                .setMsgType(msgType)
                .setContent(content)
                .setReceiveId(receiveId);
        String data = JacksonUtils.serialize(feishuSendUserMsgDTO);
        Map<String, String> headerParams = new HashMap<>() {{
            put("Content-Type", "application/json; charset=utf-8");
            put("Authorization", "Bearer " + accessToken);
        }};
        Map<String, String> requestParams = Map.of("receive_id_type", receiveIdType);
        String url;
        if (StringUtils.isNotEmpty(messageId)) {
            url = String.format(FeishuConstants.FEISHU_REPLY_USER_URL, messageId);
            feishuSendUserMsgDTO.setReceiveId(null);
        } else {
            url = FeishuConstants.FEISHU_SEND_MESSAGE_TO_USER_URL;
        }
        String result = HttpUtils.postForm(url, data, headerParams, requestParams);
        FeishuP2pResponseDTO feishuResponse = JacksonUtils.deserialize(result, FeishuP2pResponseDTO.class);
        if (!Objects.equals(feishuResponse.getCode(), 0) || Objects.nonNull(feishuResponse.getError())) {
            log.error("飞书机器人发送消息失败，请求体: {}, message: {}, error: {}", data, feishuResponse.getMsg(), feishuResponse.getError());
            throw new FeishuP2pException(feishuResponse.getMsg() + "，" + feishuResponse.getError(), receiveId, headerParams, requestParams);
        }
        log.info("飞书机器人发送消息，result: {}", result);
        return feishuResponse;
    }

    @TimedExecution(methodDescription = "获取飞书tenant_access_token")
    public String getTenantAccessToken() {
        String accessToken = redisUtils.get(FEISHU_OPENAI_REDIS_KEY);
        if (Objects.isNull(accessToken)) {
            String data = JacksonUtils.serialize(new HashMap<>() {{
                put("app_id", feishuConfig.getFeishuOpenaiAppId());
                put("app_secret", feishuConfig.getFeishuOpenaiAppSecret());
            }});
            String result = HttpUtils.postForm(FeishuConstants.FEISHU_GET_TENANT_ACCESS_TOKEN_URL, data);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (Objects.nonNull(jsonObject)) {
                accessToken = jsonObject.getString("tenant_access_token");
                redisUtils.set("tenantAccessToken", accessToken, tenantExpireTime, TimeUnit.MILLISECONDS);
            }
        }
        return accessToken;
    }

    private String getContent(String content) {
        Map<String, String> data = new HashMap<>();
        data.put("text", content);
        return JacksonUtils.serialize(data);
    }

    // 第一个 %s 是 markdown 格式，不加双引号，要使用 json 工具类序列化一遍来转义内部的符号
    private static final String CHAT_RESPONSE_TEMPLATE = """
            {
                "zh_cn": {
                    "content": [
                        [
                            {
                                "tag": "md",
                                "text": %s
                            }
                        ],
                        [
                            {
                                "tag": "hr"
                            }
                        ],
                        [
                            {
                                "tag": "text",
                                "text": "模型：%s，total_tokens：%s，price：$%s，耗时：%.2f秒"
                            }
                        ]
                    ]
                }
            }
            """;

    public String getContent(ChatResponseBO chatResponseBO) {
        String content = chatResponseBO.getContent();
        String model = chatResponseBO.getModel();
        Integer totalTokens = chatResponseBO.getTotalTokens();
        BigDecimal price = chatResponseBO.getPrice();
        Double executeTime = chatResponseBO.getExecuteTime();
        return String.format(CHAT_RESPONSE_TEMPLATE, JacksonUtils.serialize(content), model, totalTokens, price, executeTime);
    }

    public void handleP2pException(FeishuP2pException e) {
        FeishuSendUserMsgDTO feishuSendUserMsgDTO = new FeishuSendUserMsgDTO();
        feishuSendUserMsgDTO.setContent(getContent(e.getMessage()));
        feishuSendUserMsgDTO.setMsgType("text");
        feishuSendUserMsgDTO.setReceiveId(e.getReceiveId());
        String data = JacksonUtils.serialize(feishuSendUserMsgDTO);
        HttpUtils.postForm(FeishuConstants.FEISHU_SEND_MESSAGE_TO_USER_URL, data, e.getHeaderParams(), e.getRequestParams());
    }

    public void handleWeather(WeatherInfoBO weather, String receiveId, String receiveType, String msgType) {
        String content = getContent(weather);
        FeishuClient self = SpringUtil.getBean(FeishuClient.class);
        self.sendFeishuP2pMsg(content, receiveId, receiveType, msgType, null);
    }


    public String getContent(WeatherInfoBO weather) {
        String cardHeader = getCardHeader(weather.getLocationName());
        StringBuilder cardBuilder = new StringBuilder(cardHeader);

        for (int j = 0; j < weather.getDailyWeathers().size(); j++) {
            WeatherInfoBO.DailyWeather dailyWeather = weather.getDailyWeathers().get(j);
            cardBuilder.append(getDailyWeatherContent(dailyWeather));

            // 添加分隔符，避免在最后一个天气信息后再添加
            if (j < weather.getDailyWeathers().size() - 1) {
                cardBuilder.append(", { \"tag\": \"hr\" },");
            } else {
                cardBuilder.append(", { \"tag\": \"hr\" }");
            }
        }

        cardBuilder.append(getFooter(weather.getSuggestion()));
        return cardBuilder.toString();
    }

    private String getCardHeader(String locationName) {
        return String.format("""
        {
            "config": { "wide_screen_mode": true },
            "header": {
                "title": { "tag": "plain_text", "content": "%s" }
            },
            "elements": [
                { "tag": "div", "fields": [
                    { "is_short": false, "text": { "tag": "plain_text", "content": "近3天天气预报：" } }
                ] },
                { "tag": "hr" },
        """, "\uD83C\uDF24️ 天气预报 - " + locationName);
    }

    private String getDailyWeatherContent(WeatherInfoBO.DailyWeather dailyWeather) {
        return String.format("""
        { "tag": "div", "fields": [
            { "is_short": false, "text": { "tag": "lark_md", "content": "**📅 日期**: %s" } }
        ] },
        { "tag": "div", "fields": [
            { "is_short": true, "text": { "tag": "lark_md", "content": "**🌅 日出**: %s" } },
            { "is_short": true, "text": { "tag": "lark_md", "content": "**🌇 日落**: %s" } }
        ] },
        { "tag": "div", "fields": [
            { "is_short": true, "text": { "tag": "lark_md", "content": "**🌡️ 最高温**: %s°C" } },
            { "is_short": true, "text": { "tag": "lark_md", "content": "**❄️ 最低温**: %s°C" } }
        ] },
        { "tag": "div", "fields": [
            { "is_short": true, "text": { "tag": "lark_md", "content": "**💧 降水量**: %s mm" } },
            { "is_short": true, "text": { "tag": "lark_md", "content": "**👁️ 能见度**: %s km" } }
        ] },
        { "tag": "div", "fields": [
            { "is_short": true, "text": { "tag": "lark_md", "content": "**🌬️ 白天风速**: %s km/h" } },
            { "is_short": true, "text": { "tag": "lark_md", "content": "**🌙 夜晚风速**: %s km/h" } }
        ] },
        { "tag": "div", "fields": [
            { "is_short": true, "text": { "tag": "lark_md", "content": "**🌤️ 白天天气**: %s" } },
            { "is_short": true, "text": { "tag": "lark_md", "content": "**🌜 夜晚天气**: %s" } }
        ] }
        """,
                dailyWeather.getFxDate(),
                dailyWeather.getSunrise(),
                dailyWeather.getSunset(),
                dailyWeather.getTempMax(),
                dailyWeather.getTempMin(),
                dailyWeather.getPrecip(),
                dailyWeather.getVis(),
                dailyWeather.getWindSpeedDay(),
                dailyWeather.getWindSpeedNight(),
                dailyWeather.getTextDay(),
                dailyWeather.getTextNight());
    }

    private String getFooter(String suggestion) {
        return String.format("""
        ,{
            "tag": "div",
            "fields": [
                {
                    "is_short": false,
                    "text": {
                        "tag": "lark_md",
                        "content": %s
                    }
                }
            ]
        }
        ]
    }
    """, JacksonUtils.serialize("**温馨提示**: " + suggestion));
    }

    public String uploadImage(String base64, String receiveId, String receiveType) {
        String accessToken = getTenantAccessToken();
        File base64ToTempFile;
        try {
            base64ToTempFile = convertBase64ToTempFile(base64);
        } catch (IOException e) {
            log.error("base64 字符串转临时文件失败， base64: {}", base64, e);
            throw new FeishuP2pException("飞书获取图片失败", receiveId, receiveType);
        }

        String result = HttpUtils.postFormData(FeishuConstants.FEISHU_UPLOAD_IMAGE_URL,
                Map.of(
                        "image", base64ToTempFile
                ),
                Map.of(
                        "Authorization", "Bearer " + accessToken,
                        "Content-Type", "multipart/form-data"
                ),
                Map.of(
                        "image_type", "message"
                ));
        FeishuUploadResponseDTO response = JacksonUtils.deserialize(result, FeishuUploadResponseDTO.class);
        if (response.getCode() != 0) {
            log.error("飞书上传图片失败，result: {}", result);
            throw new FeishuP2pException("上传图片失败，" + response.getMsg(), receiveId, receiveType);
        }
        return response.getData().getImageKey();
    }

    public static File convertBase64ToTempFile(String base64Str) throws IOException {
        // 解码 base64 字符串为字节数组
        byte[] decodedBytes = Base64.getDecoder().decode(base64Str);

        // 创建临时文件。系统会自动在默认的临时目录下创建该文件。
        File tempFile = Files.createTempFile("image_", ".png").toFile();

        // 将字节写入临时文件
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(decodedBytes);
        }

        return tempFile;
    }

    public void sendImageMessage(String imageKey, String receiveId, String receiveType, String messageId) {
        FeishuClient self = SpringUtil.getBean(FeishuClient.class);
        self.sendFeishuP2pMsg(getImageKeyContent(imageKey), receiveId, receiveType, "image", messageId);
    }

    private String getImageKeyContent(String imageKey) {
        return JacksonUtils.serialize(new HashMap<>() {{
            put("image_key", imageKey);
        }});
    }

    public void sendTpP2pMessage(String threadPoolInfo, String receiveId, String receiveType, String messageId) {
        FeishuClient self = SpringUtil.getBean(FeishuClient.class);
        self.sendFeishuP2pMsg(buildMessageCard(threadPoolInfo), receiveId, receiveType, "interactive", messageId);
    }

    private static final String THREAD_POOL_INFO_CARD = """
            {
                "config": {
                    "wide_screen_mode": true
                },
                "header": {
                    "title": {
                        "tag": "plain_text",
                        "content": "openai对话线程池快照 ——— %s"
                    },
                    "template": "blue"
                },
                "elements": [
                    {
                        "tag": "div",
                        "fields": [
                            {
                                "text": {
                                    "tag": "lark_md",
                                    "content": %s
                                }
                            }
                        ]
                    }
                ]
            }
            """;

    public static String buildMessageCard(String threadPoolInfo) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = now.format(formatter);
        return String.format(THREAD_POOL_INFO_CARD, formattedDate, JacksonUtils.serialize(threadPoolInfo));
    }

    public void delayRenewCard(String card, String token) {
        String accessToken = getTenantAccessToken();
        String body = """
                {
                    "token": "%s",
                    "card": %s
                }
                """;
        feishuRenewCardProducer.sendRenewCardMessage(new FeishuDelayRenewCardBO(String.format(body, token, card), accessToken),
                1,
                TimeUnit.SECONDS);
    }

    public void sendPromptConfigCard(String receiveId, String receiveType, String messageId) {
        List<PromptDO> promptDOS = promptMapper.selectList(new LambdaQueryWrapper<PromptDO>()
                .select(PromptDO::getAct, PromptDO::getId));
        List<FeishuComboboxOptionBO> options = promptDOS.stream()
                .map(promptDO -> new FeishuComboboxOptionBO(promptDO.getAct(), promptDO.getId().toString()))
                .toList();
        StringBuilder optionsStringBuilder = new StringBuilder("[");
        for (int i = 0; i < options.size(); i++) {
            FeishuComboboxOptionBO option = options.get(i);
            optionsStringBuilder.append("{")
                    .append("\"text\": { \"tag\": \"plain_text\", \"content\": \"")
                    .append(option.getText())
                    .append("\" },")
                    .append("\"value\": \"")
                    .append(option.getValue())
                    .append("\" }");
            if (i < options.size() - 1) {
                optionsStringBuilder.append(",");
            }
        }
        optionsStringBuilder.append("]");

        String cardTemplate = """
                {
                    "schema": "2.0",
                    "config": {
                        "update_multi": true,
                        "style": {
                            "text_size": {
                                "normal_v2": {
                                    "default": "normal",
                                    "pc": "normal",
                                    "mobile": "heading"
                                }
                            }
                        }
                    },
                    "body": {
                        "direction": "horizontal",
                        "horizontal_spacing": "8px",
                        "vertical_spacing": "8px",
                        "horizontal_align": "left",
                        "vertical_align": "top",
                        "padding": "12px 12px 12px 12px",
                        "elements": [
                            {
                                "tag": "column_set",
                                "horizontal_align": "left",
                                "columns": [
                                    {
                                        "tag": "column",
                                        "width": "weighted",
                                        "elements": [
                                            {
                                                "tag": "markdown",
                                                "content": "选择提示词",
                                                "text_align": "left",
                                                "text_size": "normal_v2"
                                            }
                                        ],
                                        "vertical_spacing": "8px",
                                        "horizontal_align": "left",
                                        "vertical_align": "top",
                                        "weight": 1
                                    },
                                    {
                                        "tag": "column",
                                        "width": "weighted",
                                        "elements": [
                                            {
                                                "tag": "select_static",
                                                "placeholder": {
                                                    "tag": "plain_text",
                                                    "content": "请选择"
                                                },
                                                "options": {OPTIONS},
                                                "type": "default",
                                                "width": "default"
                                            }
                                        ],
                                        "vertical_spacing": "8px",
                                        "horizontal_align": "left",
                                        "vertical_align": "top",
                                        "weight": 1
                                    }
                                ],
                                "margin": "0px 0px 0px 0px"
                            },
                            {
                                "tag": "button",
                                "text": {
                                    "tag": "plain_text",
                                    "content": "提交"
                                },
                                "type": "primary",
                                "width": "default",
                                "size": "medium",
                                "confirm": {
                                    "title": {
                                        "tag": "plain_text",
                                        "content": ""
                                    },
                                    "text": {
                                        "tag": "plain_text",
                                        "content": "是否确认提交"
                                    }
                                },
                                "behaviors": [
                                    {
                                        "type": "callback",
                                        "value": {
                                            "type": "PROMPT_CONFIG_SUBMIT"
                                        }
                                    }
                                ],
                                "margin": "0px 0px 0px 0px"
                            }
                        ]
                    },
                    "header": {
                        "title": {
                            "tag": "plain_text",
                            "content": "请设置您的常用提示词"
                        },
                        "subtitle": {
                            "tag": "plain_text",
                            "content": ""
                        },
                        "template": "wathet",
                        "padding": "12px 12px 12px 12px"
                    }
                }
                """;
        String card = cardTemplate.replace("{OPTIONS}", optionsStringBuilder.toString());
        FeishuP2pResponseDTO response = sendFeishuP2pMsg(card, receiveId, receiveType, "interactive", messageId);
        userPromptSubmissionsMapper.insert(new UserPromptSubmissionsDO(null, response.getData().getMessageId(), receiveId, null, null));

    }

    public void sendRefreshSuccessMsg(String receiveId, String receiveType, String messageId) {
        sendFeishuP2pMsg(getContent("机器人上下文已重置"), receiveId, receiveType, "text", messageId);
    }

    public void sendP2PPredictQuestion(String nextQuestionsStr, String receiveId, String receiveIdType, String msgType, String messageId) {
        FeishuClient self = SpringUtil.getBean(FeishuClient.class);
        self.sendFeishuP2pMsg(getNextQuestionsCard(nextQuestionsStr), receiveId, receiveIdType, msgType, messageId);
    }

    public String getNextQuestionsCard(String nextQuestionsStr) {
        //预测用户肯能使用的问题的模板
        String predictQuestionCard = """
                {
                    "type":"template",
                    "data":{
                        "template_id":"AAqS7rtKi7JKR",
                        "template_version_name":"1.0.1",
                        "template_variable":%s
                    }
                }
                """;
        if (StringUtils.isEmpty(nextQuestionsStr) || StringUtils.equals(nextQuestionsStr, "无")) {
            log.info("没有下一个问题：{}", nextQuestionsStr);
            return "";
        }
        //将问题字符串根据空格分割
        String[] questionSplitList = nextQuestionsStr.split("\n");
        log.info("预测用户可能使用的问题：{}", Arrays.toString(questionSplitList));
        List<Map<String, String>> questions = Arrays.stream(questionSplitList).map(question -> Map.of("question", question)).toList();
        return String.format(predictQuestionCard, JacksonUtils.serialize(Map.of("questions", questions)));
    }

}

