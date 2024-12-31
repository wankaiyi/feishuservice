package com.wky.feishuservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wky.feishuservice.model.bo.WeatherInfoBO;
import com.wky.feishuservice.model.po.LocationDO;

/**
 * @author wky
 * @date 2024/10/12
 */
public interface LocationService extends IService<LocationDO> {

    WeatherInfoBO getWeather(String locationName);
}
