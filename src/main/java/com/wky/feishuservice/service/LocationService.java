package com.wky.feishuservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import groovy.lang.Tuple2;

/**
 * @author wky
 * @date 2024/10/12
 */
public interface LocationService extends IService<LocationDO> {

    Tuple2<LocationDO, WeatherResponseDTO> getWeather(String locationName);
}
