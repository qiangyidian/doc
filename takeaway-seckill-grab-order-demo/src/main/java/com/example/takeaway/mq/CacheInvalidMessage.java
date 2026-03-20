package com.example.takeaway.mq;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CacheInvalidMessage {

    // 发生变更的菜品 ID。
    private Long mealId;

    // 商家 ID，用来删除热门菜单缓存。
    private Long merchantId;

    // 删除缓存的原因，例如 UPDATE_MEAL、UPDATE_STOCK。
    private String reason;

    // 消息创建时间。
    private LocalDateTime eventTime;
}