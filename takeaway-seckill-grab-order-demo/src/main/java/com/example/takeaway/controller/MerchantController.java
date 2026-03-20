package com.example.takeaway.controller;

import com.example.takeaway.common.Result;
import com.example.takeaway.dto.UpdateMealInfoRequest;
import com.example.takeaway.dto.UpdateMealStockRequest;
import com.example.takeaway.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PutMapping("/merchant/meals/{mealId}")
    public Result<String> updateMealInfo(@PathVariable Long mealId,
                                         @RequestBody UpdateMealInfoRequest request) {
        merchantService.updateMealInfo(mealId, request);
        return Result.success("菜品信息已更新到 MySQL，请再触发 Canal 模拟接口删除缓存");
    }

    @PutMapping("/merchant/meals/{mealId}/stock")
    public Result<String> updateMealStock(@PathVariable Long mealId,
                                          @RequestBody UpdateMealStockRequest request) {
        merchantService.updateMealStock(mealId, request.getStock());
        return Result.success("库存已更新到 MySQL，请再触发 Canal 模拟接口删除缓存");
    }

    @PostMapping("/canal/mock/cache-invalid/{mealId}")
    public Result<String> mockCanal(@PathVariable Long mealId,
                                    @RequestParam(defaultValue = "MANUAL_INVALID") String reason) {
        merchantService.mockCanalSendCacheInvalid(mealId, reason);
        return Result.success("模拟 Canal 已发送删缓存消息");
    }
}