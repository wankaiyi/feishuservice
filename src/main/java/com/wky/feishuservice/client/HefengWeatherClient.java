package com.wky.feishuservice.client;

import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import com.wky.feishuservice.utils.HttpUtils;
import com.wky.feishuservice.utils.JacksonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * 和风天气客户端，参考<a href="https://dev.qweather.com/docs/start/">和风天气开发文档</a>
 *
 * @author wankaiyi
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HefengWeatherClient {

    public static final String HEFENG_3DAYS_WEATHER_URL = "https://devapi.qweather.com/v7/weather/3d";

    @Value("${hefeng.private-key}")
    public String HEFENG_PRIVATE_KEY;
    public static final String LANGUAGE_ZH = "zh";

    public WeatherResponseDTO getWeather(LocationDO location) {
        String res = HttpUtils.get(HEFENG_3DAYS_WEATHER_URL, new HashMap<>() {{
            put("location", location.getId());
            put("key", HEFENG_PRIVATE_KEY);
            put("lang", LANGUAGE_ZH);
        }});
        return JacksonUtils.deserialize(res, WeatherResponseDTO.class);
    }
}
