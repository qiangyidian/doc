package com.example.takeaway.service.impl;

import com.example.takeaway.dto.UpdateMealInfoRequest;
import com.example.takeaway.entity.Meal;
import com.example.takeaway.mapper.MealMapper;
import com.example.takeaway.mapper.MealStockMapper;
import com.example.takeaway.mq.CacheInvalidProducer;
import com.example.takeaway.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MealMapper mealMapper;
    private final MealStockMapper mealStockMapper;
    private final CacheInvalidProducer cacheInvalidProducer;

    @Override
    public void updateMealInfo(Long mealId, UpdateMealInfoRequest request) {
        // 商家改菜品时，只改 MySQL。
        // 不直接删缓存，这是 Cache-Aside 的核心思想之一。
        Meal meal = new Meal();
        meal.setMealName(request.getMealName());
        meal.setMealDesc(request.getMealDesc());
        meal.setPrice(request.getPrice());
        meal.setHotFlag(request.getHotFlag());
        meal.setStatus(request.getStatus());
        mealMapper.updateMealInfo(mealId, meal);
    }

    @Override
    public void updateMealStock(Long mealId, Integer stock) {
        // 更新库存同样只操作 MySQL。
        mealStockMapper.updateStock(mealId, stock);
    }

    @Override
    public void mockCanalSendCacheInvalid(Long mealId, String reason) {
        // 这里模拟的是：
        // MySQL 写入成功 -> Canal 监听 Binlog -> 发送删缓存消息
        Meal meal = mealMapper.selectBasicById(mealId);
        if (meal == null) {
            throw new IllegalArgumentException("菜品不存在，无法发送删缓存消息");
        }
        cacheInvalidProducer.send(mealId, meal.getMerchantId(), reason);
    }
}