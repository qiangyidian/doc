package com.example.takeaway.service.impl;

import com.example.takeaway.common.RedisKeyConstants;
import com.example.takeaway.entity.Meal;
import com.example.takeaway.mapper.MealMapper;
import com.example.takeaway.service.CacheService;
import com.example.takeaway.service.MealService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final CacheService cacheService;
    private final MealMapper mealMapper;
    private final RedissonClient redissonClient;

    @Override
    public Meal queryMealDetail(Long mealId) {
        // 第一步：先查布隆过滤器。
        if (!cacheService.mightContainMealId(mealId)) {
            throw new IllegalArgumentException("菜品不存在，已被布隆过滤器拦截");
        }

        // 第二步：查 Redis 缓存。
        Meal cacheMeal = cacheService.getMealCache(mealId);
        if (cacheMeal != null) {
            return cacheMeal;
        }

        // 第三步：缓存没命中，开始做缓存击穿保护。
        RLock lock = redissonClient.getLock(RedisKeyConstants.rebuildCacheLockKey(mealId));
        boolean locked = false;
        try {
            // 这里没有主动指定 leaseTime，所以 Redisson Watch Dog 可以自动续期。
            locked = lock.tryLock(2, TimeUnit.SECONDS);
            if (locked) {
                Meal dbMeal = mealMapper.selectMealDetailById(mealId);
                if (dbMeal == null) {
                    throw new IllegalArgumentException("菜品不存在");
                }
                cacheService.cacheMeal(dbMeal);
                return dbMeal;
            }

            // 没拿到锁的线程，短暂休眠后重试 Redis。
            Thread.sleep(100);
            Meal retryCacheMeal = cacheService.getMealCache(mealId);
            if (retryCacheMeal != null) {
                return retryCacheMeal;
            }

            // 最后再兜底查一次数据库。
            Meal dbMeal = mealMapper.selectMealDetailById(mealId);
            if (dbMeal != null) {
                cacheService.cacheMeal(dbMeal);
            }
            return dbMeal;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询菜品详情时线程被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}