package com.example.multithread.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchExportResponse {

    // 总记录数。
    private Integer totalSize;

    // 实际导出的分片结果。
    private List<String> chunkResultList;

    // 总耗时。
    private long costMillis;
}