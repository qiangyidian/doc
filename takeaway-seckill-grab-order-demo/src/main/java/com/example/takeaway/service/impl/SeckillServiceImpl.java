package com.example.takeaway.service.impl;

import com.example.takeaway.common.RedisKeyConstants;
import com.example.takeaway.dto.SeckillOrderRequest;
import com.example.takeaway.entity.Meal;
import com.example.takeaway.entity.OrderInfo;
import com.example.takeaway.mapper.MealMapper;
import com.example.takeaway.service.OrderService;
import com.example.takeaway.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> stockOrderRedisScript;
    private final MealMapper mealMapper;
    private final OrderService orderService;

    @Override
    public String createOrder(SeckillOrderRequest request) {
        // 第一层加锁：模拟 createOrder() 方法先拿到商品锁。
        RLock lock = redissonClient.getLock(RedisKeyConstants.mealLockKey(request.getMealId()));
        // 注意：这里调用的是 lock()，没有指定租约时间。
        // 这样 Redisson 的 Watch Dog 就会自动续期。
        lock.lock();
        try {
            return deductStockAndCreateOrder(request);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String deductStockAndCreateOrder(SeckillOrderRequest request) {
        // 第二层再次获取同一把锁，演示 Redisson 的可重入锁机制。
        RLock lock = redissonClient.getLock(RedisKeyConstants.mealLockKey(request.getMealId()));
        lock.lock();
        try {
            String orderNo = "ODR-" + UUID.randomUUID().toString().replace("-", "");

            // 这两个 key 使用了相同的 Hash Tag，保证落到同一槽位。
            String stockKey = RedisKeyConstants.stockKey(request.getMealId());
            String orderKey = RedisKeyConstants.orderMarkerKey(request.getMealId(), request.getUserId());

            Long result = stringRedisTemplate.execute(
                    stockOrderRedisScript,
                    List.of(stockKey, orderKey)
            );

            if (result == null) {
                throw new IllegalStateException("Lua 脚本执行失败，返回值为空");
            }
            if (result == 0L) {
                throw new IllegalStateException("库存已抢完");
            }
            if (result == 2L) {
                throw new IllegalStateException("你已经抢过这份秒杀商品了");
            }
            if (result == 3L) {
                throw new IllegalStateException("Redis 中不存在库存 key，请先做缓存预热");
            }

            // Redis 侧扣减库存成功后，再落 MySQL 订单。
            Meal meal = mealMapper.selectMealDetailById(request.getMealId());
            if (meal == null) {
                throw new IllegalArgumentException("菜品不存在");
            }

            OrderInfo orderInfo = orderService.createOrder(
                    request.getUserId(),
                    request.getMealId(),
                    orderNo,
                    meal.getPrice()
            );

            return orderInfo.getOrderNo();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}