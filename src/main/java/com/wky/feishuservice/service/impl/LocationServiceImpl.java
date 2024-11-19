package com.wky.feishuservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wky.feishuservice.client.HefengWeatherClient;
import com.wky.feishuservice.mapper.LocationMapper;
import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import com.wky.feishuservice.service.LocationService;
import groovy.lang.Tuple2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wky
 * @date 2024/10/12
 */
@Service
@RequiredArgsConstructor
public class LocationServiceImpl extends ServiceImpl<LocationMapper, LocationDO>
        implements LocationService {

    private final HefengWeatherClient hefengWeatherClient;
    private final LocationMapper locationMapper;

    @Override
    public Tuple2<LocationDO, WeatherResponseDTO> getWeather(String locationName) {
        List<LocationDO> locationDOS = locationMapper.selectList(new LambdaQueryWrapper<LocationDO>()
                .likeRight(LocationDO::getLocationName, locationName));
        LocationDO location = locationDOS.get(0);
        WeatherResponseDTO weather = hefengWeatherClient.getWeather(location);
        return new Tuple2<>(location, weather);

    }
}




