package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用片段响应体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceChunkResponse {

    /**
     * 知识来源文件的原始名称
     */
    private String sourceFileName;

    /**
     * 知识来源文件的存储路径
     */
    private String sourcePath;

    /**
     * 知识切片在该文件中的顺序索引
     */
    private Integer chunkIndex;

    /**
     * 知识切片的具体文本内容
     */
    private String content;

    /**
     * 该切片的检出来源（如向量检索、BM25 检索等）
     */
    private String retrievalSource;

    /**
     * 多路召回融合或排序后的最终得分
     */
    private Double fusionScore;
}
