package com.example.multithread.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CacheWarmService {

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledWarm() {
        // 真实业务里，这里可能是预热首页推荐、热门商品、配置缓存。
        log.info("线程={}，开始执行凌晨缓存预热任务", Thread.currentThread().getName());
        doWarm("scheduled");
    }

    @Async("businessExecutor")
    public void manualWarm() {
        // 手动触发时，也走线程池异步执行，避免接口卡顿。
        log.info("线程={}，收到手动缓存预热任务", Thread.currentThread().getName());
        doWarm("manual");
    }

    private void doWarm(String source) {
        try {
            Thread.sleep(1500);
            log.info("线程={}，缓存预热完成，来源={}", Thread.currentThread().getName(), source);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("缓存预热线程被中断", e);
        }
    }
}