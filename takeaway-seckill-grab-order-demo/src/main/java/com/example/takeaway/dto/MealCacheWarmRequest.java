package com.example.takeaway.dto;

import lombok.Data;

@Data
public class MealCacheWarmRequest {

    // 允许为空。
    // 如果传了商家 ID，就只预热这个商家的热门菜单。
    // 如果不传，就预热全部热门菜单。
    private Long merchantId;
}