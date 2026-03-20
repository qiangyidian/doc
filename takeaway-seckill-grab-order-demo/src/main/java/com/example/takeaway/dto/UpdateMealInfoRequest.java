package com.example.takeaway.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateMealInfoRequest {

    // 修改后的菜品名称。
    private String mealName;

    // 修改后的菜品描述。
    private String mealDesc;

    // 修改后的价格。
    private BigDecimal price;

    // 是否热门。
    private Integer hotFlag;

    // 上下架状态。
    private Integer status;
}