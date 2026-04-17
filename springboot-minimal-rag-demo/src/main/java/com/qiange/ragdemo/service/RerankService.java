package com.qiange.ragdemo.service;

import com.qiange.ragdemo.service.model.RetrievedChunk;

import java.util.List;

/**
 * 第三阶段精排服务。
 */
public interface RerankService {

    /**
     * 对混合检索候选片段做精排。
     *
     * @param question 用户问题
     * @param candidateChunks 候选片段
     * @return 精排后的片段
     */
    List<RetrievedChunk> rerank(String question, List<RetrievedChunk> candidateChunks);
}
