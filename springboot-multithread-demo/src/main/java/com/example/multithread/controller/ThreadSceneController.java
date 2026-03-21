package com.example.multithread.controller;

import com.example.multithread.common.Result;
import com.example.multithread.dto.AsyncJobRequest;
import com.example.multithread.dto.BatchExportResponse;
import com.example.multithread.dto.OrderSummaryResponse;
import com.example.multithread.dto.StockDeductRequest;
import com.example.multithread.service.AggregateQueryService;
import com.example.multithread.service.AsyncNoticeService;
import com.example.multithread.service.BatchExportService;
import com.example.multithread.service.CacheWarmService;
import com.example.multithread.service.LimitedInvokeService;
import com.example.multithread.service.LocalStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/thread")
public class ThreadSceneController {

    private final AggregateQueryService aggregateQueryService;
    private final AsyncNoticeService asyncNoticeService;
    private final BatchExportService batchExportService;
    private final LocalStockService localStockService;
    private final LimitedInvokeService limitedInvokeService;
    private final CacheWarmService cacheWarmService;

    @GetMapping("/order-summary/{orderId}")
    public Result<OrderSummaryResponse> orderSummary(@PathVariable Long orderId) {
        return Result.success(aggregateQueryService.queryOrderSummary(orderId));
    }

    @PostMapping("/async-notice")
    public Result<String> asyncNotice(@Valid @RequestBody AsyncJobRequest request) {
        asyncNoticeService.sendOrderNotice(request.getOrderNo(), request.getPhone());
        return Result.success("主线程已快速返回，通知任务正在后台异步执行");
    }

    @GetMapping("/export-report")
    public Result<BatchExportResponse> exportReport() {
        return Result.success(batchExportService.exportReport());
    }

    @PostMapping("/stock/deduct")
    public Result<String> deductStock(@Valid @RequestBody StockDeductRequest request) {
        return Result.success(localStockService.deductStock(request.getCount()));
    }

    @GetMapping("/stock/current")
    public Result<Integer> currentStock() {
        return Result.success(localStockService.currentStock());
    }

    @GetMapping("/limited-task")
    public Result<String> limitedTask() {
        return Result.success(limitedInvokeService.invokeHeavyTask());
    }

    @PostMapping("/cache-warm/manual")
    public Result<String> manualWarm() {
        cacheWarmService.manualWarm();
        return Result.success("缓存预热任务已异步提交");
    }
}