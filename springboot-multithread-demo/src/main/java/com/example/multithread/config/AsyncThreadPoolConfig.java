package com.example.multithread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncThreadPoolConfig {

    @Bean("businessExecutor")
    public Executor businessExecutor() {
        // 这个线程池用于普通业务异步任务，例如：
        // 接口聚合查询、异步通知、定时预热。
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：平时一直保留的线程数。
        executor.setCorePoolSize(4);
        // 最大线程数：高峰期最多能扩到多少线程。
        executor.setMaxPoolSize(8);
        // 队列容量：任务先进入队列，队列满了再扩线程。
        executor.setQueueCapacity(100);
        // 空闲线程存活时间，单位秒。
        executor.setKeepAliveSeconds(60);
        // 线程名前缀，方便你在日志里区分到底是谁在执行任务。
        executor.setThreadNamePrefix("biz-exec-");
        // 当队列满、线程也满时，让提交任务的调用线程自己执行。
        // 这种策略比直接丢任务更安全，适合学习阶段。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("exportExecutor")
    public Executor exportExecutor() {
        // 这个线程池专门给批量导出这种“拆分子任务”的场景使用。
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("export-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}