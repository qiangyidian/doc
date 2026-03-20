package com.example.takeaway.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Meal {

    // 菜品主键。
    private Long id;

    // 商家 ID。
    private Long merchantId;

    // 菜品名称。
    private String mealName;

    // 菜品描述。
    private String mealDesc;

    // 菜品价格。
    private BigDecimal price;

    // 是否热门菜品，1 表示热门。
    private Integer hotFlag;

    // 状态，1 表示上架。
    private Integer status;

    // 这里把库存一起查出来，方便返回给前端。
    private Integer stock;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}