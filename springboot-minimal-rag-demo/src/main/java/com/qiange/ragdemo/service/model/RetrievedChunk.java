package com.qiange.ragdemo.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

/**
 * 融合检索后的内部片段模型。
 *
 * 作为多路召回中间处理过程中的载体，封装文档实例以及融合时的来源和得分。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {

    /**
     * 该片段在融合池里的唯一键（防止同个片段被多路多次插入）
     */
    private String uniqueKey;

    /**
     * 召回的具体文档块对象，里面包含了本文内容和元数据
     */
    private Document document;

    /**
     * 该片段是被哪一路召回上来的（VECTOR, KEYWORD, VECTOR+KEYWORD）
     */
    private String retrievalSource;

    /**
     * 采用 RRF（倒数秩融合）计算的累加得分，分越高代表相关度可能越高
     */
    private Double fusionScore;
}
