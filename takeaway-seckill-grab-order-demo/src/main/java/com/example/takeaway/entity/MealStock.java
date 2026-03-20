package com.example.takeaway.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MealStock {

    private Long id;

    // 对应菜品 ID。
    private Long mealId;

    // 当前库存。
    private Integer stock;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}