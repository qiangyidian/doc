package com.example.multithread.service;

import com.example.multithread.dto.OrderSummaryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AggregateQueryService {

    private final MockRemoteQueryService mockRemoteQueryService;

    // 这里注入我们自定义的业务线程池。
    private final Executor businessExecutor;

    public AggregateQueryService(MockRemoteQueryService mockRemoteQueryService,
                                 @Qualifier("businessExecutor") Executor businessExecutor) {
        this.mockRemoteQueryService = mockRemoteQueryService;
        this.businessExecutor = businessExecutor;
    }

    public OrderSummaryResponse queryOrderSummary(Long orderId) {
        long start = System.currentTimeMillis();

        // 任务 1：异步查询用户信息。
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(
                () -> mockRemoteQueryService.queryUserName(orderId),
                businessExecutor
        );

        // 任务 2：异步查询价格信息。
        CompletableFuture<String> priceFuture = CompletableFuture.supplyAsync(
                () -> mockRemoteQueryService.queryPriceInfo(orderId),
                businessExecutor
        );

        // 任务 3：异步查询库存信息。
        CompletableFuture<String> stockFuture = CompletableFuture.supplyAsync(
                () -> mockRemoteQueryService.queryStockInfo(orderId),
                businessExecutor
        );

        // allOf 的作用是：等上面 3 个任务全部执行完成。
        CompletableFuture.allOf(userFuture, priceFuture, stockFuture).join();

        OrderSummaryResponse response = new OrderSummaryResponse();
        response.setOrderId(orderId);
        response.setUserName(userFuture.join());
        response.setPriceInfo(priceFuture.join());
        response.setStockInfo(stockFuture.join());
        response.setCostMillis(System.currentTimeMillis() - start);
        return response;
    }
}