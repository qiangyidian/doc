package com.example.multithread.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class LimitedInvokeService {

    // 这里只允许最多 3 个任务同时进入。
    private final Semaphore semaphore = new Semaphore(3);

    public String invokeHeavyTask() {
        boolean acquired = false;
        try {
            // 尝试拿许可。
            acquired = semaphore.tryAcquire();
            if (!acquired) {
                return "当前并发任务过多，请稍后再试";
            }

            log.info("线程={}，拿到许可，开始执行重量任务", Thread.currentThread().getName());
            Thread.sleep(2000);
            return "执行成功，线程=" + Thread.currentThread().getName();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("限流任务被中断", e);
        } finally {
            // 只有真正拿到许可的线程才归还许可。
            if (acquired) {
                semaphore.release();
            }
        }
    }
}