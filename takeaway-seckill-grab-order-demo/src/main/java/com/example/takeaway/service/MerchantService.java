package com.example.takeaway.service;

import com.example.takeaway.dto.UpdateMealInfoRequest;

public interface MerchantService {

    void updateMealInfo(Long mealId, UpdateMealInfoRequest request);

    void updateMealStock(Long mealId, Integer stock);

    void mockCanalSendCacheInvalid(Long mealId, String reason);
}