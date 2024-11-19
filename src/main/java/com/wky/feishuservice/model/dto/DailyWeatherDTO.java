package com.wky.feishuservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wky
 * @date 2024/10/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyWeatherDTO {
    private String fxDate;
    private String sunrise;
    private String sunset;
    private String moonrise;
    private String moonset;
    private String moonPhase;
    private String moonPhaseIcon;
    private String tempMax;
    private String tempMin;
    private String iconDay;
    /**
     * 白天天气状况的描述，包括阴晴雨雪等天气状态的描述
     */
    private String textDay;
    private String iconNight;
    private String textNight;
    private String wind360Day;
    private String windDirDay;
    private String windScaleDay;
    private String windSpeedDay;
    private String wind360Night;
    private String windDirNight;
    private String windScaleNight;
    private String windSpeedNight;
    private String humidity;
    /**
     * 降水量，默认单位：毫米
     */
    private String precip;
    private String pressure;
    /**
     * 能见度
     */
    private String vis;
    private String cloud;
    private String uvIndex;
}
