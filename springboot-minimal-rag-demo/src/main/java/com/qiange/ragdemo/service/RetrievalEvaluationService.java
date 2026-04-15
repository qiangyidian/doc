package com.qiange.ragdemo.service;

import com.qiange.ragdemo.dto.RetrievalEvaluationResponse;

/**
 * 检索评估服务。
 */
public interface RetrievalEvaluationService {

    /**
     * 执行检索评估。
     *
     * @return 评估结果
     */
    RetrievalEvaluationResponse evaluate();
}
