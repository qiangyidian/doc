package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 第三阶段问答响应体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AskQuestionResponse {

    /**
     * 大模型生成的回答内容
     */
    private String answer;

    /**
     * RAG 重写或扩展后的查询词列表
     */
    private List<String> rewrittenQueries;

    /**
     * 压缩前上下文总长度。
     */
    private Integer contextLengthBeforeCompression;

    /**
     * 压缩后上下文总长度。
     */
    private Integer contextLengthAfterCompression;

    /**
     * 生成回答所参考的知识片段列表
     */
    private List<ReferenceChunkResponse> references;
}
