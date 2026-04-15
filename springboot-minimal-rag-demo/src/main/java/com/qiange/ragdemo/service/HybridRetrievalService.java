package com.qiange.ragdemo.service;

import com.qiange.ragdemo.service.model.RetrievedChunk;

import java.util.List;

/**
 * 混合检索服务。
 */
public interface HybridRetrievalService {

    /**
     * 根据多路查询词和分类执行混合检索并融合结果。
     *
     * @param queries 多路查询词
     * @param category 分类
     * @return 检索到的文档片段列表
     */
    List<RetrievedChunk> retrieve(List<String> queries, String category);
}
