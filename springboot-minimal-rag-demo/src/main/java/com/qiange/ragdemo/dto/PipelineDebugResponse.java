package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 第三阶段流水线调试响应体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineDebugResponse {

    private List<String> queries;

    private List<ReferenceChunkResponse> retrievedChunks;

    private List<ReferenceChunkResponse> rerankedChunks;

    private List<ReferenceChunkResponse> compressedChunks;

    private Integer contextLengthBeforeCompression;

    private Integer contextLengthAfterCompression;
}
