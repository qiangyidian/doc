package com.example.lockdemo.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

@Service
public class StampedLockPriceService {

    private final Map<Long, Integer> priceMap = new HashMap<>();

    // StampedLock 适合“读非常多，写比较少”的场景。
    private final StampedLock stampedLock = new StampedLock();

    public int getPrice(Long productId) {
        // 先尝试乐观读，不会真正阻塞写线程。
        long stamp = stampedLock.tryOptimisticRead();
        Integer price = priceMap.get(productId);

        // 如果乐观读期间发生了写操作，validate 会失败。
        if (!stampedLock.validate(stamp)) {
            // 乐观读失败后，再退化成悲观读锁。
            stamp = stampedLock.readLock();
            try {
                price = priceMap.get(productId);
            } finally {
                stampedLock.unlockRead(stamp);
            }
        }

        return price == null ? 0 : price;
    }

    public void updatePrice(Long productId, Integer price) {
        long stamp = stampedLock.writeLock();
        try {
            priceMap.put(productId, price);
        } finally {
            stampedLock.unlockWrite(stamp);
        }
    }
}