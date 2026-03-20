package com.example.takeaway.service.impl;

import com.example.takeaway.common.RedisKeyConstants;
import com.example.takeaway.entity.Meal;
import com.example.takeaway.mapper.MealMapper;
import com.example.takeaway.service.CacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final MealMapper mealMapper;

    @Value("${takeaway.cache.meal-ttl-minutes}")
    private long mealTtlMinutes;

    @Value("${takeaway.cache.hot-meal-ttl-hours}")
    private long hotMealTtlHours;

    @Value("${takeaway.cache.random-ttl-minutes-min}")
    private long randomTtlMinMinutes;

    @Value("${takeaway.cache.random-ttl-minutes-max}")
    private long randomTtlMaxMinutes;

    @Value("${takeaway.bloom.bitmap-size}")
    private long bloomBitmapSize;

    // 这里直接把多个 hash 种子定义死，方便新手理解。
    private static final int[] BLOOM_SEEDS = {5, 7, 11, 13, 31, 37, 61};

    @PostConstruct
    public void initBloomFilter() {
        // 项目启动时，把所有合法 mealId 先放进布隆过滤器。
        rebuildMealBloomFilter();
    }

    @Override
    public Meal getMealCache(Long mealId) {
        String json = stringRedisTemplate.opsForValue().get(RedisKeyConstants.mealKey(mealId));
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Meal.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析菜品缓存失败", e);
        }
    }

    @Override
    public void cacheMeal(Meal meal) {
        try {
            String json = objectMapper.writeValueAsString(meal);
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.mealKey(meal.getId()),
                    json,
                    buildMealTtl()
            );
            // 缓存菜品详情时，顺手把库存 key 也一起准备好。
            prepareStockKey(meal);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("写入菜品缓存失败", e);
        }
    }

    @Override
    public void warmHotMeals(Long merchantId) {
        List<Meal> hotMeals = mealMapper.selectHotMeals(merchantId);
        for (Meal meal : hotMeals) {
            cacheMeal(meal);
            putMealIdIntoBloom(meal.getId());
        }

        if (merchantId != null) {
            try {
                String json = objectMapper.writeValueAsString(hotMeals);
                stringRedisTemplate.opsForValue().set(
                        RedisKeyConstants.hotMealsKey(merchantId),
                        json,
                        buildHotMealTtl()
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException("写入热门菜单缓存失败", e);
            }
        }
    }

    @Override
    public void rebuildMealBloomFilter() {
        stringRedisTemplate.delete(RedisKeyConstants.mealBloomKey());
        List<Long> mealIds = mealMapper.selectAllValidMealIds();
        for (Long mealId : mealIds) {
            putMealIdIntoBloom(mealId);
        }
        log.info("布隆过滤器重建完成，mealId 数量={}", mealIds.size());
    }

    @Override
    public boolean mightContainMealId(Long mealId) {
        String bloomKey = RedisKeyConstants.mealBloomKey();
        for (int seed : BLOOM_SEEDS) {
            long offset = hash(String.valueOf(mealId), seed);
            Boolean exists = stringRedisTemplate.opsForValue().getBit(bloomKey, offset);
            if (Boolean.FALSE.equals(exists)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void prepareStockKey(Meal meal) {
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.stockKey(meal.getId()),
                String.valueOf(meal.getStock()),
                buildMealTtl()
        );
    }

    @Override
    public List<Meal> getHotMealsCache(Long merchantId) {
        String json = stringRedisTemplate.opsForValue().get(RedisKeyConstants.hotMealsKey(merchantId));
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readerForListOf(Meal.class).readValue(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析热门菜单缓存失败", e);
        }
    }

    private void putMealIdIntoBloom(Long mealId) {
        String bloomKey = RedisKeyConstants.mealBloomKey();
        for (int seed : BLOOM_SEEDS) {
            long offset = hash(String.valueOf(mealId), seed);
            stringRedisTemplate.opsForValue().setBit(bloomKey, offset, true);
        }
    }

    private long hash(String value, int seed) {
        long result = 0;
        for (int i = 0; i < value.length(); i++) {
            result = result * seed + value.charAt(i);
        }
        return Math.abs(result) % bloomBitmapSize;
    }

    private Duration buildMealTtl() {
        long randomMinutes = ThreadLocalRandom.current()
                .nextLong(randomTtlMinMinutes, randomTtlMaxMinutes + 1);
        return Duration.ofMinutes(mealTtlMinutes + randomMinutes);
    }

    private Duration buildHotMealTtl() {
        long randomMinutes = ThreadLocalRandom.current()
                .nextLong(randomTtlMinMinutes, randomTtlMaxMinutes + 1);
        return Duration.ofHours(hotMealTtlHours).plusMinutes(randomMinutes);
    }
}