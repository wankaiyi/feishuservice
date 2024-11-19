package com.wky.feishuservice.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.client.OpenaiClient;
import com.wky.feishuservice.enumurations.ReceiveType;
import com.wky.feishuservice.exceptions.FeishuP2pException;
import com.wky.feishuservice.exceptions.OpenAiException;
import com.wky.feishuservice.model.bo.ChatResponseBO;
import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import com.wky.feishuservice.service.FeishuMessageService;
import com.wky.feishuservice.utils.RedisUtils;
import groovy.lang.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author wky
 * @date 2024/11/11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeishuMessageServiceImpl implements FeishuMessageService {

    private final FeishuClient feishuClient;
    private final RedisUtils redisUtils;
    private final OpenaiClient openaiClient;
    private final LocationServiceImpl locationServiceImpl;

    @Override
    public JSONObject processFeishuNotice(JSONObject body) {
        JSONObject res = new JSONObject();
        if ("url_verification".equals(body.get("type"))) {
            res.put("challenge", body.getString("challenge"));
            return res;
        }
        log.info("飞书通知接口被请求，请求体为：{}", body.toJSONString());
        //处理消息
        handleMessage(body);
        res.put("challenge", body.getString("challenge"));
        return res;
    }

    public void handleMessage(JSONObject body) {
        JSONObject header = body.getJSONObject("header");
        long createTime = Long.parseLong(header.getString("create_time"));
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime createTimeLDT = Instant.ofEpochMilli(createTime)
                .atZone(zoneId)
                .toLocalDateTime();
        if (createTimeLDT.isBefore(LocalDateTime.now().minusMinutes(5))) {
            // 超过5分钟的消息不处理
            return;
        }
        JSONObject event = body.getJSONObject("event");
        JSONObject message = event.getJSONObject("message");
        JSONObject sender = event.getJSONObject("sender");
        String eventType = header.getString("event_type");
        // 判断是否为接收到用户消息
        if ("im.message.receive_v1".equals(eventType)) {
            String eventId = header.getString("event_id");
            JSONObject content = JSONObject.parseObject(message.getString("content"));
            String contentText = content.getString("text").replaceAll("@_user_1 ", "");
            JSONObject senderId = sender.getJSONObject("sender_id");
            String senderOpenId = senderId.getString("open_id");


            // 对于同一个事件，只处理一次
            if (Objects.nonNull(redisUtils.get("feishu:eventId:" + eventId))) {
                log.info("事件重复，不处理，eventId: {}", eventId);
                return;
            }
            //缓存event_id
            redisUtils.set("feishu:eventId:" + eventId, eventId, 60 * 60 * 1000, TimeUnit.MILLISECONDS);

            // 私聊直接回复
            if ("p2p".equals(message.getString("chat_type"))) {
                handleTextMessage(message, contentText, ReceiveType.OPEN_ID.getValue(), senderOpenId);
            }
        }
    }

    //处理文本消息
    private void handleTextMessage(JSONObject message, String contentText, String receiveType, String receiveId) {
        // 不是文本消息，不处理
        if (!"text".equals(message.getString("message_type"))) {
            throw new FeishuP2pException("暂不支持其他消息类型", receiveId, receiveType);
        }
        if (contentText.startsWith("#天气")) {
            // 只保留中文字符
            String location = contentText.substring(3).replaceAll("[^\\u4e00-\\u9fa5]", "");
            Tuple2<LocationDO, WeatherResponseDTO> locationAndWeather = locationServiceImpl.getWeather(location);
            feishuClient.handelWeather(locationAndWeather, receiveId, receiveType, "interactive");
        } else {
            try {
                ChatResponseBO chatResponseBO = openaiClient.chat(receiveId, contentText);
                feishuClient.sendP2pMsg(chatResponseBO, receiveId, receiveType, "post", message.getString("message_id"));
            } catch (OpenAiException e) {
                throw new FeishuP2pException(e.getMessage(), receiveId, receiveType);
            }
        }
    }
}
