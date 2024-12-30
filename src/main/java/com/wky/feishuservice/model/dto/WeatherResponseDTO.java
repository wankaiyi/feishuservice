package com.wky.feishuservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author wky
 * @date 2024/10/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherResponseDTO {
    private String code;
    private String updateTime;
    private String fxLink;
    @JsonProperty("daily")
    private List<DailyWeatherDTO> dailyWeathers;
    private Refer refer;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Refer {
        private List<String> sources;
        private List<String> license;
    }
}

