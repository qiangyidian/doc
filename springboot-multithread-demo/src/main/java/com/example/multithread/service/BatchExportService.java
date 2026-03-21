package com.example.multithread.service;

import com.example.multithread.dto.BatchExportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class BatchExportService {

    private final Executor exportExecutor;

    public BatchExportService(@Qualifier("exportExecutor") Executor exportExecutor) {
        this.exportExecutor = exportExecutor;
    }

    public BatchExportResponse exportReport() {
        long start = System.currentTimeMillis();

        // 模拟一份 50 条数据的报表。
        List<Integer> dataList = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            dataList.add(i);
        }

        // 每 10 条数据切成一片，总共 5 个分片。
        int chunkSize = 10;
        int taskCount = (int) Math.ceil(dataList.size() * 1.0 / chunkSize);

        // 线程安全的结果容器。
        List<String> resultList = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch countDownLatch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            int startIndex = i * chunkSize;
            int endIndex = Math.min(startIndex + chunkSize, dataList.size());
            List<Integer> subList = dataList.subList(startIndex, endIndex);

            exportExecutor.execute(() -> {
                try {
                    // 模拟导出这个分片。
                    Thread.sleep(500);
                    String result = "线程=" + Thread.currentThread().getName()
                            + "，导出分片=" + subList;
                    resultList.add(result);
                    log.info(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("导出任务被中断", e);
                } finally {
                    // 不管成功还是失败，都要 countDown。
                    countDownLatch.countDown();
                }
            });
        }

        try {
            // 主线程在这里等待，直到所有分片导出完成。
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("主线程等待导出结果时被中断", e);
        }

        BatchExportResponse response = new BatchExportResponse();
        response.setTotalSize(dataList.size());
        response.setChunkResultList(resultList);
        response.setCostMillis(System.currentTimeMillis() - start);
        return response;
    }
}