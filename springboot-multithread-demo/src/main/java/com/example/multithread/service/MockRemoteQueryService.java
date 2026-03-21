package com.example.multithread.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockRemoteQueryService {

    public String queryUserName(Long orderId) {
        sleep(300);
        log.info("线程={}，完成用户信息查询", Thread.currentThread().getName());
        return "用户-" + orderId;
    }

    public String queryPriceInfo(Long orderId) {
        sleep(400);
        log.info("线程={}，完成价格信息查询", Thread.currentThread().getName());
        return "价格：99.00元";
    }

    public String queryStockInfo(Long orderId) {
        sleep(500);
        log.info("线程={}，完成库存信息查询", Thread.currentThread().getName());
        return "库存：充足";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程休眠被中断", e);
        }
    }
}