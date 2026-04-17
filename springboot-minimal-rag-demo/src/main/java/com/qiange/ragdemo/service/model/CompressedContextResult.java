package com.qiange.ragdemo.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 上下文压缩结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompressedContextResult {

    private List<RetrievedChunk> chunks;

    private Integer contextLengthBeforeCompression;

    private Integer contextLengthAfterCompression;
}
