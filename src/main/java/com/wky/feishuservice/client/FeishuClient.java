package com.wky.feishuservice.client;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wky.feishuservice.annotation.TimedExecution;
import com.wky.feishuservice.constants.FeishuConstants;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.mapper.PromptMapper;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.bo.FeishuComboboxOptionBO;
import com.wky.feishuservice.model.bo.FeishuDelayRenewCardBO;
import com.wky.feishuservice.model.dto.DailyWeatherDTO;
import com.wky.feishuservice.model.dto.FeishuP2pResponseDTO;
import com.wky.feishuservice.model.dto.FeishuSendUserMsgDTO;
import com.wky.feishuservice.model.dto.FeishuUploadResponseDTO;
import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import com.wky.feishuservice.model.po.PromptDO;
import com.wky.feishuservice.producer.FeishuRenewCardProducer;
import com.wky.feishuservice.utils.HttpUtils;
import com.wky.feishuservice.utils.JacksonUtils;
import com.wky.feishuservice.utils.RedisUtils;
import groovy.lang.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private final RedisUtils redisUtils;
    private final PromptMapper promptMapper;
    private final FeishuRenewCardProducer feishuRenewCardProducer;

    public void sendP2pMsg(ChatResponseBO chatResponseBO, String receiveId, String receiveIdType, String msgType, String messageId) {
        FeishuClient self = SpringUtil.getBean(FeishuClient.class);
        self.sendFeishuP2pMsg(getContent(chatResponseBO), receiveId, receiveIdType, msgType, messageId);
    }

    @TimedExecution(methodDescription = "飞书发送点对点消息")
    public void sendFeishuP2pMsg(String content, String receiveId, String receiveIdType, String msgType, String messageId) {
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
        Map<String, String> requestParams = new HashMap<>() {{
            put("receive_id_type", receiveIdType);

        }};
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
            log.error("飞书机器人发送消息失败，message: {}, error: {}", feishuResponse.getMsg(), feishuResponse.getError());
            throw new FeishuP2pException(feishuResponse.getMsg() + "，" + feishuResponse.getError(), receiveId, headerParams, requestParams);
        }
        log.info("飞书机器人发送消息，result: {}", result);
    }

    @TimedExecution(methodDescription = "获取飞书tenant_access_token")
    public String getTenantAccessToken() {
        String accessToken = redisUtils.get(FEISHU_OPENAI_REDIS_KEY);
        if (Objects.isNull(accessToken)) {
            String data = JacksonUtils.serialize(new HashMap<>() {{
                put("app_id", FeishuConstants.FEISHU_OPENAI_APP_ID);
                put("app_secret", FeishuConstants.FEISHU_OPENAI_APP_SECRET);
            }});
            String result = HttpUtils.postForm(FeishuConstants.FEISHU_GET_TENANT_ACCESS_TOKEN_URL, data);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (Objects.nonNull(jsonObject)) {
                accessToken = jsonObject.getString("tenant_access_token");
                redisUtils.set("tenantAccessToken", accessToken, 90 * 60 * 1000, TimeUnit.MILLISECONDS);
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
                                "text": "模型：%s，total_tokens：%s，price：$%s"
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
        return String.format(CHAT_RESPONSE_TEMPLATE, JacksonUtils.serialize(content), model, totalTokens, price);
    }

    public void handelP2pException(FeishuP2pException e) {
        FeishuSendUserMsgDTO feishuSendUserMsgDTO = new FeishuSendUserMsgDTO();
        feishuSendUserMsgDTO.setContent(getContent(e.getMessage()));
        feishuSendUserMsgDTO.setMsgType("text");
        feishuSendUserMsgDTO.setReceiveId(e.getReceiveId());
        String data = JacksonUtils.serialize(feishuSendUserMsgDTO);
        HttpUtils.postForm(FeishuConstants.FEISHU_SEND_MESSAGE_TO_USER_URL, data, e.getHeaderParams(), e.getRequestParams());
    }

    public void handelWeather(Tuple2<LocationDO, WeatherResponseDTO> locationAndWeather, String receiveId, String receiveType, String msgType) {
        String content = getContent(locationAndWeather);
        FeishuClient self = SpringUtil.getBean(FeishuClient.class);
        self.sendFeishuP2pMsg(content, receiveId, receiveType, msgType, null);
    }


    private String getContent(Tuple2<LocationDO, WeatherResponseDTO> locationAndWeather) {
        LocationDO location = locationAndWeather.getV1();
        WeatherResponseDTO weather = locationAndWeather.getV2();
        List<DailyWeatherDTO> dailyWeathers = weather.getDailyWeathers();
        return createWeatherCard(location.getLocationName(), dailyWeathers);
    }

    private static String createWeatherCard(String cityName, List<DailyWeatherDTO> weatherList) {
        String cardHeader = """
                {
                    "config": {
                        "wide_screen_mode": true
                    },
                    "header": {
                        "title": {
                            "tag": "plain_text",
                            "content": "%s"
                        }
                    },
                    "elements": [
                        { "tag": "div", "fields": [
                            { "is_short": false, "text": { "tag": "plain_text", "content": "近3天天气预报：" } }
                        ] },
                        { "tag": "hr" },
                """;

        StringBuilder cardBuilder = new StringBuilder(String.format(cardHeader, "\uD83C\uDF24️ 天气预报 - " + cityName));

        // 使用文本块来定义每个天气块
        String weatherTemplate = """
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
                """;


        // 添加每天的天气预报
        for (int j = 0; j < weatherList.size(); j++) {
            DailyWeatherDTO weather = weatherList.get(j);
            cardBuilder.append(String.format(weatherTemplate,
                    weather.getFxDate(),
                    weather.getSunrise(),
                    weather.getSunset(),
                    weather.getTempMax(),
                    weather.getTempMin(),
                    weather.getPrecip(),
                    weather.getVis(),
                    weather.getWindSpeedDay(),
                    weather.getWindSpeedNight(),
                    weather.getTextDay(),
                    weather.getTextNight()));
            if (j < weatherList.size() - 1) {
                cardBuilder.append("""
                        ,
                                            { "tag": "hr" },
                        """);
            }
        }

        // 为JSON添加闭合符号
        cardBuilder.append("""
                    ]
                }
                """);
        return cardBuilder.toString();
    }

    public String uploadImage(String base64, String receiveId, String receiveType) {
        String accessToken = getTenantAccessToken();
        String result = HttpUtils.postFormData(FeishuConstants.FEISHU_UPLOAD_IMAGE_URL, new HashMap<>() {{
                    put("image", convertBase64ToTempFile(base64));
                }},
                new HashMap<>() {{
                    put("Authorization", "Bearer " + accessToken);
                    put("Content-Type", "multipart/form-data");
                }},
                new HashMap<>() {{
                    put("image_type", "message");
                }});
        FeishuUploadResponseDTO response = JacksonUtils.deserialize(result, FeishuUploadResponseDTO.class);
        if (response.getCode() != 0) {
            log.error("飞书上传图片失败，result: {}", result);
            throw new FeishuP2pException("上传图片失败，" + response.getMsg(), receiveId, receiveType);
        }
        return response.getData().getImageKey();
    }

    public static File convertBase64ToTempFile(String base64Str) {
        try {
            // 解码 base64 字符串为字节数组
            byte[] decodedBytes = Base64.getDecoder().decode(base64Str);

            // 创建临时文件。系统会自动在默认的临时目录下创建该文件。
            File tempFile = Files.createTempFile("image_", ".png").toFile();

            // 将字节写入临时文件
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(decodedBytes);
            }

            return tempFile;
        } catch (IOException e) {
            log.error("base64 字符串转临时文件失败， base64Str: {}", base64Str, e);
            return null;
        }
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
        List<PromptDO> promptDOS = promptMapper.selectList(new LambdaQueryWrapper<PromptDO>());
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
                                            "": ""
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
        sendFeishuP2pMsg(card, receiveId, receiveType, "interactive", messageId);

    }
}
