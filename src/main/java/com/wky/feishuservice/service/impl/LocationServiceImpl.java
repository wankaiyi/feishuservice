package com.wky.feishuservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wky.feishuservice.client.HefengWeatherClient;
import com.wky.feishuservice.client.OpenAiClient;
import com.wky.feishuservice.mapper.LocationMapper;
import com.wky.feishuservice.model.bo.WeatherInfoBO;
import com.wky.feishuservice.model.dto.DailyWeatherDTO;
import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import com.wky.feishuservice.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wky
 * @date 2024/10/12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl extends ServiceImpl<LocationMapper, LocationDO>
        implements LocationService {

    private final HefengWeatherClient hefengWeatherClient;
    private final LocationMapper locationMapper;
    private final OpenAiClient openAiClient;

    @Override
    public WeatherInfoBO getWeather(String locationName) {
        if (StringUtils.isBlank(locationName)) {
            return null;
        }
        List<LocationDO> locationDOS = locationMapper.selectList(new LambdaQueryWrapper<LocationDO>()
                .likeRight(LocationDO::getLocationName, locationName));
        if (locationDOS.isEmpty()) {
            log.info("未查询到地区，locationName:{}", locationName);
            return null;
        }
        LocationDO location = locationDOS.get(0);
        WeatherResponseDTO weather = hefengWeatherClient.getWeather(location.getId());
        WeatherInfoBO weatherInfoBO = new WeatherInfoBO()
                .setLocationName(location.getLocationName())
                .setDailyWeathers(
                        weather.getDailyWeathers().stream()
                                .map(DailyWeatherDTO::buildDailyWeather)
                                .toList()
                );
        log.info("获取天气成功，location:{}, weather:{}", location, weather);
        // 根据天气获取建议
        String weatherSuggestion = openAiClient.getWeatherSuggestion(weatherInfoBO.getDailyWeathers());
        weatherInfoBO.setSuggestion(weatherSuggestion);
        return weatherInfoBO;
    }
}




