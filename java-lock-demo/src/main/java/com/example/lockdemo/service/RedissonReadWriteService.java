package com.example.lockdemo.service;

import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedissonReadWriteService {

    private final RedissonClient redissonClient;

    public RedissonReadWriteService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public String readConfig(String key) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("lock:config:" + key);
        rwLock.readLock().lock();
        try {
            return "读取配置成功，key=" + key;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public String writeConfig(String key, String value) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("lock:config:" + key);
        rwLock.writeLock().lock();
        try {
            return "写入配置成功，key=" + key + ", value=" + value;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}