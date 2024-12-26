package com.wky.feishuservice.producer;

import com.wky.feishuservice.constants.FeishuConstants;
import com.wky.feishuservice.model.bo.FeishuDelayRenewCardBO;
import com.wky.feishuservice.utils.HttpUtils;
import com.wky.feishuservice.utils.JacksonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 延迟更新卡片，因为飞书在响应之前都不能调用修改消息或更新卡片的接口，所以需要延迟一段时间再调用接口，暂时没有想到更好的方法
 *
 * @author wky
 * @date 2024/12/26
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeishuRenewCardProducer {

    private final RedissonClient redissonClient;

    RBlockingDeque<String> queue;
    RDelayedQueue<String> delayedQueue;


    @PostConstruct
    public void init() {
        queue = redissonClient.getBlockingDeque("feishu:renew_card:queue");
        delayedQueue = redissonClient.getDelayedQueue(queue);
        new Thread(() -> {
            while (true) {
                FeishuDelayRenewCardBO task;
                try {
                    String pollTask = queue.poll(10, TimeUnit.SECONDS);
                    if (Objects.isNull(pollTask)) {
                        continue;
                    }
                    task = JacksonUtils.deserialize(pollTask, FeishuDelayRenewCardBO.class);
                    if (Objects.nonNull(task)) {
                        String accessToken = task.getAccessToken();
                        Map<String, String> headerParams = new HashMap<>() {{
                            put("Content-Type", "application/json; charset=utf-8");
                            put("Authorization", "Bearer " + accessToken);
                        }};
                        HttpUtils.postForm( FeishuConstants.FEISHU_DELAY_UPDATE_CARD_URL, task.getBody(), headerParams);
                    }
                } catch (InterruptedException e) {
                    log.info("消费者被中断，退出循环");
                    break;
                }
            }
        }).start();
    }

    public void sendRenewCardMessage(FeishuDelayRenewCardBO tuple2, long delayTime, TimeUnit timeUnit) {
        delayedQueue.offer(JacksonUtils.serialize(tuple2), delayTime, timeUnit);
    }
}
