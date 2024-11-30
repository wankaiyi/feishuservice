package com.wky.feishuservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author wky
 * @date 2024/11/30
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "feishuservice.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

}
