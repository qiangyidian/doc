package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检索调试响应体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalDebugResponse {

    /**
     * 重写或生成的用于检索的查询词列表
     */
    private List<String> queries;

    /**
     * 根据查询词检索并排序后返回的知识片段集合
     */
    private List<ReferenceChunkResponse> chunks;
}
