package com.example.takeaway.service;

import com.example.takeaway.entity.Meal;

import java.util.List;

public interface CacheService {

    Meal getMealCache(Long mealId);

    void cacheMeal(Meal meal);

    void warmHotMeals(Long merchantId);

    void rebuildMealBloomFilter();

    boolean mightContainMealId(Long mealId);

    void prepareStockKey(Meal meal);

    List<Meal> getHotMealsCache(Long merchantId);
}