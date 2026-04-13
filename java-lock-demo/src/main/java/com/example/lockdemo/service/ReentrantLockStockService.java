package com.example.lockdemo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ReentrantLockStockService {

    private int stock = 20;

    // 这里创建的是可重入锁，默认是非公平锁。
    private final ReentrantLock lock = new ReentrantLock();

    public int deduct(int count) throws InterruptedException {
        if (count <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于 0");
        }

        // tryLock 可以让我们在获取不到锁时快速失败，而不是一直死等。
        boolean success = lock.tryLock(3, TimeUnit.SECONDS);
        if (!success) {
            throw new RuntimeException("获取锁超时，请稍后重试");
        }

        try {
            if (stock < count) {
                throw new RuntimeException("库存不足，当前库存=" + stock);
            }

            stock -= count;
            return stock;
        } finally {
            // ReentrantLock 一定要在 finally 里手动释放。
            lock.unlock();
        }
    }

    public int currentStock() {
        return stock;
    }
}