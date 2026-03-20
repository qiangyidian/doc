package com.example.takeaway.service;

import com.example.takeaway.entity.Meal;

public interface MealService {

    Meal queryMealDetail(Long mealId);
}