package com.example.lockdemo.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class ReadWriteConfigService {

    // 用 Map 模拟“配置缓存”。
    private final Map<String, String> configMap = new HashMap<>();

    // 读写锁：适合读多写少场景。
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public String read(String key) {
        // 读操作加读锁，多个读线程可以并发进入。
        readWriteLock.readLock().lock();
        try {
            return configMap.getOrDefault(key, "未配置");
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void write(String key, String value) {
        // 写操作加写锁，写时不允许其他线程再读或写。
        readWriteLock.writeLock().lock();
        try {
            configMap.put(key, value);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}