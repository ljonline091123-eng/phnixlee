package com.zhaocai.business.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 业务线程池配置
 *
 * @author chenming
 * @date 2024-07-04
 */
@Configuration
@EnableAsync
public class BusinessExecutorConfig {

    @Bean(name = "businessExecutor")
    public Executor asyncServiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        /* 使用机器cpu核心 */
        int cpuCount = Runtime.getRuntime().availableProcessors();
        //配置核心线程数
        executor.setCorePoolSize(cpuCount);
        //配置最大线程数
        executor.setMaxPoolSize(cpuCount * 2 + 1);
        //配置队列大小
        executor.setQueueCapacity(1000);
        //配置线程池中的线程的名称前缀
        executor.setThreadNamePrefix("businessExecutor");

        // 拒绝策略：不在新线程中执行任务，而是有调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        //执行初始化
        executor.initialize();

        return executor;
    }
}
