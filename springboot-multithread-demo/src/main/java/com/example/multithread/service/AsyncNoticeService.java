package com.example.multithread.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AsyncNoticeService {

    //这个方法的运行是开辟一个新的线程来运行
    @Async("businessExecutor")
    public void sendOrderNotice(String orderNo, String phone) {
        // 这里的方法会在独立线程池里执行，不会阻塞主线程。
        log.info("线程={}，开始发送通知，orderNo={}", Thread.currentThread().getName(), orderNo);

        try {
            // 模拟调用短信服务、站内信服务。
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("异步通知线程被中断", e);
        }

        log.info("线程={}，通知发送完成，phone={}", Thread.currentThread().getName(), phone);
    }
}