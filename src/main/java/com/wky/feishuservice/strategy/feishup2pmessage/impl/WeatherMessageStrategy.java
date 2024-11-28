package com.wky.feishuservice.strategy.feishup2pmessage.impl;

import com.wky.feishuservice.client.FeishuClient;
import com.wky.feishuservice.enumurations.FeishuP2pPrefix;
import com.wky.feishuservice.model.common.UserInfo;
import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import com.wky.feishuservice.service.LocationService;
import com.wky.feishuservice.strategy.feishup2pmessage.FeishuP2pMessageStrategy;
import com.wky.feishuservice.utils.UserInfoContext;
import groovy.lang.Tuple2;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 处理查询天气消息策略
 *
 * @author wky
 * @date 2024/11/22
 */
@Component
@RequiredArgsConstructor
public class WeatherMessageStrategy implements FeishuP2pMessageStrategy {

    private final FeishuClient feishuClient;
    private final LocationService locationService;

    @Override
    public void handleMessage(String contentText, String messageId) {
        UserInfo userInfo = UserInfoContext.getUserInfo();
        String receiveId = userInfo.getReceiveId();
        String receiveType = userInfo.getReceiveType();
        // 只保留中文字符
        String location = contentText.substring(3).replaceAll("[^\\u4e00-\\u9fa5]", "");
        Tuple2<LocationDO, WeatherResponseDTO> locationAndWeather = locationService.getWeather(location);
        feishuClient.handelWeather(locationAndWeather, receiveId, receiveType, "interactive");
    }

    @Override
    public boolean isMatch(String contentText) {
        return StringUtils.startsWith(contentText, FeishuP2pPrefix.WEATHER.getPrefix());
    }

}
