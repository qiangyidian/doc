package com.example.takeaway.mapper;

import com.example.takeaway.entity.Meal;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MealMapper {

    Meal selectMealDetailById(@Param("mealId") Long mealId);

    List<Meal> selectHotMeals(@Param("merchantId") Long merchantId);

    List<Long> selectAllValidMealIds();

    int updateMealInfo(@Param("mealId") Long mealId,
                       @Param("meal") Meal meal);

    Meal selectBasicById(@Param("mealId") Long mealId);
}