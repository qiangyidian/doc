package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条评估样本结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalEvalCaseResult {

    /**
     * 评估样本中的测试问题
     */
    private String question;

    /**
     * 期望命中并返回的正确来源文件名
     */
    private String expectedSourceFileName;

    /**
     * 该样本是否成功在召回结果中命中期望文件（通常判断 Top-K 是否包含）
     */
    private boolean hit;

    /**
     * 如果命中，期望文件在召回结果列表中的确切排序位置（例如，排在第 1 位则 rank = 1）
     */
    private Integer rank;
}
