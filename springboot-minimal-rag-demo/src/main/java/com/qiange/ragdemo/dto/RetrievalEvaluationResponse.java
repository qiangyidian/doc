package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 召回评估结果响应体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalEvaluationResponse {

    /**
     * 参与评估的测试样本总数
     */
    private Integer totalCases;

    /**
     * Top-K 命中率（召回的 Top-K 结果中包含期望来源文档的样本比例）
     */
    private Double hitRate;

    /**
     * 平均倒数排名 (Mean Reciprocal Rank)，衡量正确结果在召回列表中的排位靠前程度
     */
    private Double mrr;

    /**
     * 每条测试样本的详细评估明细列表
     */
    private List<RetrievalEvalCaseResult> caseResults;
}
