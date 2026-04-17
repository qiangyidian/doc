package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.dto.AskQuestionRequest;
import com.qiange.ragdemo.dto.AskQuestionResponse;
import com.qiange.ragdemo.dto.ReferenceChunkResponse;
import com.qiange.ragdemo.service.ContextCompressionService;
import com.qiange.ragdemo.service.HybridRetrievalService;
import com.qiange.ragdemo.service.QueryRewriteService;
import com.qiange.ragdemo.service.RagChatService;
import com.qiange.ragdemo.service.RerankService;
import com.qiange.ragdemo.service.model.CompressedContextResult;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 第三阶段 RAG 问答服务实现。
 */
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    // 查询改写服务，用于将用户口语化提问扩展为多视角的检索词
    private final QueryRewriteService queryRewriteService;

    // 混合检索服务，用于多路召回知识片段并融合排序
    private final HybridRetrievalService hybridRetrievalService;

    // 精排服务，用于对候选片段做二次排序
    private final RerankService rerankService;

    // 上下文压缩服务，用于去重、压缩和裁剪最终上下文
    private final ContextCompressionService contextCompressionService;

    // 大模型聊天客户端，用于与底层的大语言模型通信生成最终答案
    private final ChatClient chatClient;

    /**
     * 根据用户的提问进行 RAG（检索增强生成）查询并返回答案。
     *
     * @param request 包含用户问题和（可选的）分类过滤条件
     * @return 包含大模型生成的答案和参考资料片段的响应对象
     */
    @Override
    public AskQuestionResponse ask(AskQuestionRequest request) {
        // 1. 查询重写：将原始提问重写为多条查询词，增加召回的覆盖面
        List<String> queries = queryRewriteService.rewriteQueries(request.getQuestion());
        
        // 2. 混合检索：基于多路查询词，结合向量相似度和关键词匹配检索相关片段，并使用 RRF 算法进行融合排序
        List<RetrievedChunk> chunks = hybridRetrievalService.retrieve(queries, request.getCategory());

        // 3. 校验检索结果：如果什么也没搜到，说明知识库中缺乏相关信息，直接返回提示，不再调用大模型
        if (chunks.isEmpty()) {
            return AskQuestionResponse.builder()
                    .answer("当前知识库中没有检索到足够相关的内容，请先补充知识文档或调整检索参数。")
                    .rewrittenQueries(queries)
                    .contextLengthBeforeCompression(0)
                    .contextLengthAfterCompression(0)
                    .references(List.of())
                    .build();
        }

        // 4. 精排和压缩：让最终上下文尽量短而准
        List<RetrievedChunk> rerankedChunks = rerankService.rerank(request.getQuestion(), chunks);
        CompressedContextResult compressedContextResult =
                contextCompressionService.compress(request.getQuestion(), rerankedChunks);
        List<RetrievedChunk> finalChunks = compressedContextResult.getChunks();

        if (finalChunks.isEmpty()) {
            return AskQuestionResponse.builder()
                    .answer("当前知识库中没有足够高质量的上下文支持回答该问题，请先补充知识或调整精排与压缩参数。")
                    .rewrittenQueries(queries)
                    .contextLengthBeforeCompression(compressedContextResult.getContextLengthBeforeCompression())
                    .contextLengthAfterCompression(compressedContextResult.getContextLengthAfterCompression())
                    .references(List.of())
                    .build();
        }

        // 5. 构建上下文：将最终入选片段格式化成文本，供大模型阅读
        String context = buildContext(finalChunks);

        // 6. 构建系统提示词（System Prompt），给大模型立规矩
        String systemPrompt = """
                你是一个严格基于知识库上下文回答问题的助手。
                你的回答必须遵守以下规则：
                1. 优先依据提供的知识片段回答
                2. 如果上下文不足，请明确说明“根据当前知识库无法确定”
                3. 不要编造知识库中不存在的事实
                4. 回答应尽量清晰、结构化、简洁
                """;

        // 7. 构建用户提示词（User Prompt），将“整理好的参考上下文”和“用户实际的问题”拼接在一起
        String userPrompt = """
                请基于以下高质量上下文回答问题。

                【上下文开始】
                %s
                【上下文结束】

                用户问题：
                %s
                """.formatted(context, request.getQuestion());

        // 8. 发起大模型调用（Generation）。带上系统指令和用户输入，同步等待模型生成最终答案内容
        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        // 9. 将最终入选片段转换为前端响应 DTO
        List<ReferenceChunkResponse> references = finalChunks.stream()
                .map(this::toReferenceChunkResponse)
                .toList();

        // 10. 封装并返回最终结果：答案 + 查询重写 + 上下文压缩统计 + 参考片段
        return AskQuestionResponse.builder()
                .answer(answer)
                .rewrittenQueries(queries)
                .contextLengthBeforeCompression(compressedContextResult.getContextLengthBeforeCompression())
                .contextLengthAfterCompression(compressedContextResult.getContextLengthAfterCompression())
                .references(references)
                .build();
    }

    /**
     * 将检索出的一组 Document 组装成一个用于 prompt 的大段上下文文本。
     */
    private String buildContext(List<RetrievedChunk> chunks) {
        StringBuilder builder = new StringBuilder();

        // 遍历每个文档片段，提取文本及其来源元数据进行格式化拼接
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            Document document = chunk.getDocument();
            Map<String, Object> metadata = document.getMetadata();

            builder.append("片段").append(i + 1).append("：").append("\n");
            // 加入文档来源文件名，方便模型（或者调试）知晓这段文本的来源
            builder.append("来源文件：")
                    .append(metadata.getOrDefault(RagConstants.METADATA_SOURCE_FILE_NAME, "unknown"))
                    .append("\n");
            // 调试用：显示召回来源、融合得分和精排得分
            builder.append("检索来源：").append(chunk.getRetrievalSource()).append("\n");
            builder.append("融合得分：").append(chunk.getFusionScore()).append("\n");
            builder.append("精排得分：").append(chunk.getRerankScore()).append("\n");
            builder.append(chunk.getCompressedContent()).append("\n\n");
        }

        return builder.toString();
    }

    /**
     * 将底层的 RetrievedChunk 对象映射转换为向客户端返回的 DTO 对象。
     */
    private ReferenceChunkResponse toReferenceChunkResponse(RetrievedChunk chunk) {
        Document document = chunk.getDocument();
        Map<String, Object> metadata = document.getMetadata();

        return ReferenceChunkResponse.builder()
                .sourceFileName(stringValue(metadata.get(RagConstants.METADATA_SOURCE_FILE_NAME))) // 提取源文件名
                .sourcePath(stringValue(metadata.get(RagConstants.METADATA_SOURCE_PATH)))           // 提取源文件路径
                .chunkIndex(intValue(metadata.get(RagConstants.METADATA_CHUNK_INDEX)))              // 提取分块索引
                .content(document.getText())                                                         // 提取片段正文
                .retrievalSource(chunk.getRetrievalSource())                                         // 提取检索来源
                .fusionScore(chunk.getFusionScore())                                                 // 提取融合得分
                .rerankScore(chunk.getRerankScore())                                                 // 提取精排得分
                .compressedContent(chunk.getCompressedContent())                                     // 提取压缩内容
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
