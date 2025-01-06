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
            String errorText = doPrintThreadPoolConfig(consumeThreadPool);
            log.error("触发openai对话线程池拒绝策略 {}", errorText);
            // 告警
            AlertUtils.sendErrorAlert(errorText, null);
            // 交回给调用者，CallerRunsPolicy
            if (!consumeThreadPool.isShutdown()) {
                runner.run();
            }
        });
        executor.initialize();
        return executor;
    }

    /**
     * feishu webhook 发送线程池
     */
    @Bean(name = "feishuWebhookPoolTaskExecutor")
    public ThreadPoolTaskExecutor feishuWebhookPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(5);
        executor.setCorePoolSize(2);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("feishu-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    public static String doPrintThreadPoolConfig(ThreadPoolExecutor threadPoolExecutor) {
        int corePoolSize = threadPoolExecutor.getCorePoolSize();
        int maximumPoolSize = threadPoolExecutor.getMaximumPoolSize();
        int poolSize = threadPoolExecutor.getPoolSize();
        int activeCount = threadPoolExecutor.getActiveCount();
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        long taskCount = threadPoolExecutor.getTaskCount();
        int queueSize = threadPoolExecutor.getQueue().size();
        int remainingCapacity = threadPoolExecutor.getQueue().remainingCapacity();
        return String.format("""
                        **核心线程数**：%d
                        **最大线程数**：%d
                        **当前线程数**：%d
                        **活跃线程数**：%d
                        **完成任务数**：%d
                        **总任务数**：%d
                        **队列大小**：%d
                        **剩余容量**：%d""",
                corePoolSize, maximumPoolSize, poolSize, activeCount,
                completedTaskCount, taskCount, queueSize, remainingCapacity);
    }

}
