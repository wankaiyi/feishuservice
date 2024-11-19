package com.wky.feishuservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wky
 * @date 2024/11/11
 */
@Configuration
public class RedissonConfig {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.password}")
    private String redisPassword;

    @Value("${redis.database}")
    private int redisDatabase;

    @Bean
    public RedissonClient redissonClient() {
        Config config = getConfig();
        return Redisson.create(config);
    }

    /**
     * 创建配置文件
     *
     * @return Redis配置文件
     */
    private Config getConfig() {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        // 服务器连接配置
        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress("redis://" + redisHost + ":" + redisPort);
        singleServerConfig.setDatabase(redisDatabase);
        if (redisPassword != null) {
            singleServerConfig.setPassword(redisPassword);
        }
        return config;
    }
}
