package com.example.multithread.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class LocalStockService {

    // 模拟单机库存。
    private int stock = 20;

    // 可重入锁，用来保护共享变量 stock。
    private final ReentrantLock lock = new ReentrantLock();

    public String deductStock(int count) {
        // 进入关键区前先加锁。
        lock.lock();
        try {
            if (stock < count) {
                return "库存不足，当前库存=" + stock;
            }

            // 这里故意 sleep 一下，让你更容易理解并发覆盖问题。
            Thread.sleep(300);

            stock = stock - count;
            return "扣减成功，剩余库存=" + stock + "，线程=" + Thread.currentThread().getName();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("库存扣减线程被中断", e);
        } finally {
            // finally 里释放锁，是最基本的加锁规范。
            lock.unlock();
        }
    }

    public int currentStock() {
        return stock;
    }
}