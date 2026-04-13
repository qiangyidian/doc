package com.example.lockdemo.service;

import org.springframework.stereotype.Service;

@Service
public class SynchronizedStockService {

    // 用一个成员变量模拟当前库存。
    private int stock = 20;

    public synchronized int deduct(int count) {
        // synchronized 修饰实例方法，等价于锁住当前对象 this。
        // 同一时刻只能有一个线程进入这个方法。
        if (count <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于 0");
        }

        if (stock < count) {
            throw new RuntimeException("库存不足，当前库存=" + stock);
        }

        stock -= count;
        return stock;
    }

    public synchronized int currentStock() {
        return stock;
    }
}