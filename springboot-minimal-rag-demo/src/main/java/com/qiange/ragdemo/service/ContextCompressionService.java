package com.qiange.ragdemo.service;

import com.qiange.ragdemo.service.model.CompressedContextResult;
import com.qiange.ragdemo.service.model.RetrievedChunk;

import java.util.List;

/**
 * 第三阶段上下文压缩服务。
 */
public interface ContextCompressionService {

    /**
     * 对精排后的片段做去重、压缩和裁剪。
     *
     * @param question 用户问题
     * @param rerankedChunks 精排后的片段
     * @return 压缩结果
     */
    CompressedContextResult compress(String question, List<RetrievedChunk> rerankedChunks);
}
