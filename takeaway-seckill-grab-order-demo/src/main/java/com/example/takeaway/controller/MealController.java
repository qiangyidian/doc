package com.example.takeaway.controller;

import com.example.takeaway.common.Result;
import com.example.takeaway.dto.MealCacheWarmRequest;
import com.example.takeaway.entity.Meal;
import com.example.takeaway.service.CacheService;
import com.example.takeaway.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final CacheService cacheService;

    @GetMapping("/meals/{mealId}")
    public Result<Meal> queryMeal(@PathVariable Long mealId) {
        return Result.success(mealService.queryMealDetail(mealId));
    }

    @PostMapping("/cache/warm/meals")
    public Result<String> warmMeals(@RequestBody(required = false) MealCacheWarmRequest request) {
        Long merchantId = request == null ? null : request.getMerchantId();
        cacheService.warmHotMeals(merchantId);
        return Result.success("热门菜单预热完成");
    }
}