package com.wky.feishuservice.model.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author wky
 * @date 2024/12/30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WeatherInfoBO {

    private String locationName;
    private List<DailyWeather> dailyWeathers;
    private String suggestion;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class DailyWeather {
        private String fxDate;
        private String sunrise;
        private String sunset;
        private String tempMax;
        private String tempMin;
        /**
         * 白天天气状况的描述，包括阴晴雨雪等天气状态的描述
         */
        private String textDay;
        private String textNight;
        private String windSpeedDay;
        private String windSpeedNight;
        /**
         * 降水量，默认单位：毫米
         */
        private String precip;
        /**
         * 能见度
         */
        private String vis;
    }

}
