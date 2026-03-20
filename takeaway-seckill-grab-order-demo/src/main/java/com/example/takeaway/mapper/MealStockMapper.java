package com.example.takeaway.mapper;

import com.example.takeaway.entity.MealStock;
import org.apache.ibatis.annotations.Param;

public interface MealStockMapper {

    MealStock selectByMealId(@Param("mealId") Long mealId);

    int updateStock(@Param("mealId") Long mealId,
                    @Param("stock") Integer stock);
}