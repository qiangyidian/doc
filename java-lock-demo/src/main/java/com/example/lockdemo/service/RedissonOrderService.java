package com.example.lockdemo.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedissonOrderService {

    private final RedissonClient redissonClient;

    public RedissonOrderService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public String createOrder(Long userId, Long productId) {
        // 分布式锁 key 通常要和具体业务资源绑定。
        String lockKey = "lock:order:product:" + productId;

        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            // 这里模拟真实的下单逻辑。
            // Redisson 的锁是可重入锁，同一个线程再次获取同一把锁不会死锁。
            return "下单成功，userId=" + userId + ", productId=" + productId;
        } finally {
            lock.unlock();
        }
    }
}