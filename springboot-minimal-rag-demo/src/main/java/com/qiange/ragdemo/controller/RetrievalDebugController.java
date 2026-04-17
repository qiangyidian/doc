package com.qiange.ragdemo.controller;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.common.Result;
import com.qiange.ragdemo.dto.ReferenceChunkResponse;
import com.qiange.ragdemo.dto.RetrievalDebugRequest;
import com.qiange.ragdemo.dto.RetrievalDebugResponse;
import com.qiange.ragdemo.service.HybridRetrievalService;
import com.qiange.ragdemo.service.QueryRewriteService;
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
 * 检索调试接口。
 * 提供不调用生成模型，仅测试多路召回、重写、融合等检索效果的 HTTP 端点。
 */
@RestController // 声明为 REST 控制器，返回 JSON 响应
@RequestMapping("/api/retrieval") // 定义此控制器的基础请求路径
@RequiredArgsConstructor // Lombok 注解，自动生成包含 final 字段的构造函数
public class RetrievalDebugController {

    // 注入查询重写服务
    private final QueryRewriteService queryRewriteService;

    // 注入混合检索服务（负责多路召回和 RRF 融合）
    private final HybridRetrievalService hybridRetrievalService;

    /**
     * 执行检索调试。
     * 输入用户问题，返回重写后的查询词列表，以及最终被检索并融合后排序的知识片段列表。
     * 方便在无需消耗大模型 token 的情况下验证检索质量。
     *
     * @param request 包含要测试的问题和可选分类过滤的请求对象
     * @return 包含重写后查询词和检索命中的相关知识片段详细信息的响应体
     */
    @PostMapping("/debug-search") // 映射 HTTP POST 请求到 /api/retrieval/debug-search
    public Result<RetrievalDebugResponse> debugSearch(@Valid @RequestBody RetrievalDebugRequest request) {
        // 第一步：根据用户原始问题生成一组重写后的查询词（扩展语义）
        List<String> queries = queryRewriteService.rewriteQueries(request.getQuestion());
        
        // 第二步：使用重写后的查询词进行混合检索（向量 + BM25），并融合排序结果
        List<RetrievedChunk> chunks = hybridRetrievalService.retrieve(queries, request.getCategory());

        // 第三步：将底层的检索结果模型转换为面向前端的 DTO 对象
        RetrievalDebugResponse response = RetrievalDebugResponse.builder()
                .queries(queries)
                // 遍历结果，将其映射并收集成列表返回
                .chunks(chunks.stream().map(this::toReferenceChunkResponse).toList())
                .build();

        // 统一返回成功结果包装类
        return Result.ok(response);
    }

    /**
     * 将内部的 RetrievedChunk（包含得分、来源和 Document）对象映射为接口需要的 DTO 格式。
     *
     * @param chunk 包含融合得分和来源标识的检索对象
     * @return 提取出相关信息的前端响应 DTO
     */
    private ReferenceChunkResponse toReferenceChunkResponse(RetrievedChunk chunk) {
        // 获取底层封装的 Document 对象
        Document document = chunk.getDocument();
        // 获取该文档片段绑定的元数据
        Map<String, Object> metadata = document.getMetadata();

        // 使用 Builder 模式构建响应对象，并处理空指针风险
        return ReferenceChunkResponse.builder()
                .sourceFileName(stringValue(metadata.get(RagConstants.METADATA_SOURCE_FILE_NAME)))
                .sourcePath(stringValue(metadata.get(RagConstants.METADATA_SOURCE_PATH)))
                .chunkIndex(intValue(metadata.get(RagConstants.METADATA_CHUNK_INDEX)))
                .content(document.getText())
                .retrievalSource(chunk.getRetrievalSource())
                .fusionScore(chunk.getFusionScore())
                .rerankScore(chunk.getRerankScore())
                .compressedContent(chunk.getCompressedContent())
                .finalSelectionStatus(chunk.isSelectedForAnswer()
                        ? RagConstants.PIPELINE_SELECTED
                        : RagConstants.PIPELINE_DROPPED)
                .build();
    }

    /**
     * 安全地将 Object 转换为 String，防 NullPointerException。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 安全地将 Object 转换为 Integer，防 NullPointerException。
     */
    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
