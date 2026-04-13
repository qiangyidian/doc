package com.example.lockdemo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AtomicCounterService {

    // AtomicInteger 不是传统意义上的锁，但它是高并发计数器的常见方案。
    private final AtomicInteger visitCount = new AtomicInteger(0);

    public int increment() {
        // 通过 CAS 原子更新计数值。
        return visitCount.incrementAndGet();
    }

    public int current() {
        return visitCount.get();
    }
}