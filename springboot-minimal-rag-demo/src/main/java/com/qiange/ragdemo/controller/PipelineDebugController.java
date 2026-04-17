package com.qiange.ragdemo.controller;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.common.Result;
import com.qiange.ragdemo.dto.PipelineDebugRequest;
import com.qiange.ragdemo.dto.PipelineDebugResponse;
import com.qiange.ragdemo.dto.ReferenceChunkResponse;
import com.qiange.ragdemo.service.ContextCompressionService;
import com.qiange.ragdemo.service.HybridRetrievalService;
import com.qiange.ragdemo.service.QueryRewriteService;
import com.qiange.ragdemo.service.RerankService;
import com.qiange.ragdemo.service.model.CompressedContextResult;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 第三阶段流水线调试接口。
 * 用于暴露 RAG（Retrieval-Augmented Generation）处理全流程的中间结果。
 * 包含查询重写、混合检索、重排序以及上下文压缩等各个阶段，方便开发人员排查和调优。
 */
@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineDebugController {

    // 查询重写服务：负责将用户的原始提问转化为更丰富、更易于检索的多个子查询
    private final QueryRewriteService queryRewriteService;

    // 混合检索服务：结合向量检索和全文检索等多种方式，根据查询获取相关文档块
    private final HybridRetrievalService hybridRetrievalService;

    // 重排序服务：利用特定的排序模型对初步检索回来的文档块进行打分和重新排序
    private final RerankService rerankService;

    // 上下文压缩服务：对筛选出的文档块进行内容压缩，去除冗余，以适应 LLM 的上下文窗口长度
    private final ContextCompressionService contextCompressionService;

    /**
     * 流水线调试端点。依次执行 RAG 的核心检索增强步骤，并返回每一步的状态数据。
     * 
     * @param request 包含用户问题和可能的分类过滤条件的请求对象
     * @return 包含各个阶段快照和统计信息的响应结果
     */
    @PostMapping("/debug")
    public Result<PipelineDebugResponse> debug(@Valid @RequestBody PipelineDebugRequest request) {
        // 1. 查询重写：根据原始问题生成多个检索查询
        List<String> queries = queryRewriteService.rewriteQueries(request.getQuestion());
        
        // 2. 混合检索：利用所有生成的查询，检索出相关的文档分块
        List<RetrievedChunk> retrievedChunks = hybridRetrievalService.retrieve(queries, request.getCategory());
        // 记录第一阶段（混合检索）的快照
        List<ReferenceChunkResponse> retrievedSnapshot = retrievedChunks.stream()
                .map(this::toReferenceChunkResponse)
                .toList();

        // 3. 重排序：对混合检索返回的结果基于与原始问题的相关度进行精排
        List<RetrievedChunk> rerankedChunks = rerankService.rerank(request.getQuestion(), retrievedChunks);
        // 记录第二阶段（重排序）的快照
        List<ReferenceChunkResponse> rerankedSnapshot = rerankedChunks.stream()
                .map(this::toReferenceChunkResponse)
                .toList();

        // 4. 上下文压缩：提取与问题最相关的核心内容，减少送入 LLM 的 token 消耗
        CompressedContextResult compressedContextResult =
                contextCompressionService.compress(request.getQuestion(), rerankedChunks);

        // 5. 组装响应：将整个流水线的输入、中间结果以及最终压缩结果打包返回
        PipelineDebugResponse response = PipelineDebugResponse.builder()
                .queries(queries)
                .retrievedChunks(retrievedSnapshot)
                .rerankedChunks(rerankedSnapshot)
                // 压缩后的最终文档块
                .compressedChunks(compressedContextResult.getChunks().stream().map(this::toReferenceChunkResponse).toList())
                .contextLengthBeforeCompression(compressedContextResult.getContextLengthBeforeCompression())
                .contextLengthAfterCompression(compressedContextResult.getContextLengthAfterCompression())
                .build();

        return Result.ok(response);
    }

    /**
     * 辅助方法：将检索到的文档块转换为前端用于展示或调试的响应对象（DTO）。
     * 会从中解析出元数据（如文件名、路径、分块索引等）以及各种得分。
     */
    private ReferenceChunkResponse toReferenceChunkResponse(RetrievedChunk chunk) {
        Document document = chunk.getDocument();
        Map<String, Object> metadata = document.getMetadata();

        return ReferenceChunkResponse.builder()
                // 从元数据中提取文件基本信息
                .sourceFileName(stringValue(metadata.get(RagConstants.METADATA_SOURCE_FILE_NAME)))
                .sourcePath(stringValue(metadata.get(RagConstants.METADATA_SOURCE_PATH)))
                .chunkIndex(intValue(metadata.get(RagConstants.METADATA_CHUNK_INDEX)))
                // 原始分块文本内容
                .content(document.getText())
                // 该分块的检索来源（例如：向量检索、关键字检索等）
                .retrievalSource(chunk.getRetrievalSource())
                // 混合检索时的融合得分
                .fusionScore(chunk.getFusionScore())
                // 重排序时的相关性得分
                .rerankScore(chunk.getRerankScore())
                // 如果经过了上下文压缩，这里是压缩后的文本
                .compressedContent(chunk.getCompressedContent())
                // 标记该文档块最终是否被选中参与生成答案
                .finalSelectionStatus(chunk.isSelectedForAnswer()
                        ? RagConstants.PIPELINE_SELECTED
                        : RagConstants.PIPELINE_DROPPED)
                .build();
    }

    /**
     * 安全地将对象转换为字符串，处理 null 值的辅助方法。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 安全地将对象转换为整数，处理 null 值的辅助方法。
     */
    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}