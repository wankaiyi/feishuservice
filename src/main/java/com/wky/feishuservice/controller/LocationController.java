package com.wky.feishuservice.controller;

import com.wky.feishuservice.annotation.Log;
import com.wky.feishuservice.annotation.RateLimiter;
import com.wky.feishuservice.model.dto.WeatherResponseDTO;
import com.wky.feishuservice.model.po.LocationDO;
import com.wky.feishuservice.service.LocationService;
import groovy.lang.Tuple2;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author wky
 * @date 2024/10/12
 */
@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @Log
    @RateLimiter(window = 60 * 60 * 24)//一个ip一天调10次
    @PostMapping("/weather")
    public ResponseEntity<WeatherResponseDTO> weather(@RequestParam("locationName") String locationName) {
        Tuple2<LocationDO, WeatherResponseDTO> locationAndWeather = locationService.getWeather(locationName);
        return ResponseEntity.ok().body(locationAndWeather.getV2());
    }

    private static List<LocationDO> getLocationDOS(CSVParser csvParser) {
        Iterator<CSVRecord> iterator = csvParser.iterator();
        iterator.next();
        List<LocationDO> data = new ArrayList<>();
        while (iterator.hasNext()) {
            CSVRecord csvRecord = iterator.next();
            LocationDO locationDO = new LocationDO();
            locationDO.setId(csvRecord.get(0));
            locationDO.setLocationName(csvRecord.get(2));
            locationDO.setProvince(csvRecord.get(7));
            locationDO.setLongitude(new BigDecimal(csvRecord.get(11)));
            locationDO.setLatitude(new BigDecimal(csvRecord.get(12)));
            locationDO.setAdCode(csvRecord.get(13));
            data.add(locationDO);
        }
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("upload")
    public ResponseEntity<String> upload(MultipartFile csvFile) {
        try (CSVParser csvParser = new CSVParser(new InputStreamReader(csvFile.getInputStream()), CSVFormat.DEFAULT)) {
            List<LocationDO> data = getLocationDOS(csvParser);
            locationService.saveBatch(data);
        } catch (IOException e) {
            return ResponseEntity.ok().body("Failed to parse CSV file, error" + e.getMessage());
        }

        return ResponseEntity.ok().body("success");

    }
}
