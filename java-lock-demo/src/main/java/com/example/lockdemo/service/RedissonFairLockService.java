package com.example.lockdemo.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedissonFairLockService {

    private final RedissonClient redissonClient;

    public RedissonFairLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public String grab(Long userId) {
        // 公平锁会尽量按照请求锁的先后顺序来获取锁。
        RLock fairLock = redissonClient.getFairLock("lock:grab:fair");
        fairLock.lock();
        try {
            return "公平锁抢单成功，userId=" + userId;
        } finally {
            fairLock.unlock();
        }
    }
}