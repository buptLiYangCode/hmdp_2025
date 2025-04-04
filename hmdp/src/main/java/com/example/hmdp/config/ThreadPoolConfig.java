package com.example.hmdp.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService asyncTaskExecutor() {
        // 创建基础线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        // 使用TTL包装，使其支持ThreadLocal值传递
        return TtlExecutors.getTtlExecutorService(threadPool);
    }
} 