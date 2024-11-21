package com.wky.feishuservice.config;

import com.wky.feishuservice.utils.AlertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author wky
 * @date 2024/11/21
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {

    /**
     * openai对话线程池
     * 经过测试，平均 WT/ST 约为21
     * 以某次耗时记录为例：总耗时18400ms，cpu耗时826ms，WS/ST = (18400 - 826)/826 = 21
     * 所以最大线程数可以设置为 2（核心数） * 21 = 42，由于服务器上有其他服务，所以这里就设置为40
     */
    @Bean(name = "openaiChatThreadPool")
    public ThreadPoolTaskExecutor openaiChatThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(40);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("openai-chat-thread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setRejectedExecutionHandler((runner, consumeThreadPool) -> {
            String errorText = doPrintThreadPoolConfig("openai-chat-thread-", consumeThreadPool);
            log.error("触发openai对话线程池拒绝策略 {}", errorText);
            // 告警
            AlertUtils.sendThreadPoolRejectAlert(errorText);
            // 交回给调用者，CallerRunsPolicy
            if (!consumeThreadPool.isShutdown()) {
                runner.run();
            }
        });
        executor.initialize();
        return executor;
    }

    private String doPrintThreadPoolConfig(String key, ThreadPoolExecutor threadPoolExecutor) {
        return String.join("|",
                "thread.prefix.key = " + key,
                "core.pool.size = " + threadPoolExecutor.getCorePoolSize(),
                "maximum.pool.size = " + threadPoolExecutor.getMaximumPoolSize(),
                "largest.pool.size = " + threadPoolExecutor.getLargestPoolSize(),
                "pool.size = " + threadPoolExecutor.getPoolSize(),
                "active.count = " + threadPoolExecutor.getActiveCount(),
                "queue.size = " + threadPoolExecutor.getQueue().size(),
                "queue.remain.capacity = " + threadPoolExecutor.getQueue().remainingCapacity());
    }

}
