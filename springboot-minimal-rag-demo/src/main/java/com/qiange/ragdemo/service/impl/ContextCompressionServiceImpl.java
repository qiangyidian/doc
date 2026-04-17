package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.service.ContextCompressionService;
import com.qiange.ragdemo.service.model.CompressedContextResult;
import com.qiange.ragdemo.service.model.CompressionDecision;
import com.qiange.ragdemo.service.model.CompressionResultEnvelope;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三阶段上下文压缩服务实现。
 * 该服务在检索到的文档片段（Chunks）输入给最终的大语言模型生成答案之前，
 * 对这些片段进行去重、LLM 智能压缩提炼和全局长度截断，以提升回答质量并降低 Token 消耗。
 */
@Service
@RequiredArgsConstructor
public class ContextCompressionServiceImpl implements ContextCompressionService {

    private final ChatClient chatClient;

    private final RagProperties ragProperties;

    /**
     * 压缩上下文的核心方法入口。
     *
     * @param question 用户的问题
     * @param rerankedChunks 经过重排序后的文档片段列表
     * @return 压缩后的结果，包含精简后的片段列表以及压缩前后的长度统计
     */
    @Override
    public CompressedContextResult compress(String question, List<RetrievedChunk> rerankedChunks) {
        // 如果没有检索到片段，直接返回空结果
        if (rerankedChunks == null || rerankedChunks.isEmpty()) {
            return CompressedContextResult.builder()
                    .chunks(List.of())
                    .contextLengthBeforeCompression(0)
                    .contextLengthAfterCompression(0)
                    .build();
        }

        // 计算压缩前所有片段内容的总字符长度
        int beforeLength = rerankedChunks.stream()
                .map(chunk -> safeText(chunk.getDocument().getText()))
                .mapToInt(String::length)
                .sum();

        // 1. 去重：根据配置决定是否在调用大模型压缩前对内容进行去重
        List<RetrievedChunk> deduplicatedChunks = Boolean.TRUE.equals(ragProperties.getCompression().getDeduplicateBeforeLlm())
                ? deduplicate(rerankedChunks)
                : new ArrayList<>(rerankedChunks);

        // 2. 压缩：根据配置决定是否使用大模型进行智能压缩提炼。如果不开启，则回退到基础的截断
        List<RetrievedChunk> compressedChunks = Boolean.TRUE.equals(ragProperties.getCompression().getEnabled())
                ? llmCompress(question, deduplicatedChunks)
                : truncateWithoutLlm(deduplicatedChunks);

        // 3. 限制总长度：限制最终保留的片段总字符数，防止超出大模型的上下文窗口
        List<RetrievedChunk> limitedChunks = limitTotalContext(compressedChunks);

        // 计算压缩后保留的片段内容的总字符长度
        int afterLength = limitedChunks.stream()
                .map(RetrievedChunk::getCompressedContent)
                .filter(StringUtils::hasText)
                .mapToInt(String::length)
                .sum();

        // 组装并返回最终结果
        return CompressedContextResult.builder()
                .chunks(limitedChunks)
                .contextLengthBeforeCompression(beforeLength)
                .contextLengthAfterCompression(afterLength)
                .build();
    }

    /**
     * 对检索到的文档片段进行内容去重。
     * 保持原有顺序（通常是重排后的高分在前），只保留首次出现的文本片段。
     */
    private List<RetrievedChunk> deduplicate(List<RetrievedChunk> chunks) {
        // 使用 LinkedHashMap 保持元素插入时的顺序
        Map<String, RetrievedChunk> uniqueMap = new LinkedHashMap<>();

        for (RetrievedChunk chunk : chunks) {
            // 对文本进行标准化（转小写、合并空白字符）用于判断是否重复
            String normalized = normalize(safeText(chunk.getDocument().getText()));
            if (!uniqueMap.containsKey(normalized)) {
                uniqueMap.put(normalized, chunk);
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    /**
     * 利用大语言模型 (LLM) 对文档片段进行智能压缩。
     * LLM 会评估每个片段与用户问题的相关性，并仅提取关键信息。
     */
    private List<RetrievedChunk> llmCompress(String question, List<RetrievedChunk> chunks) {
        // 配置结构化输出，要求大模型返回与 CompressionResultEnvelope 匹配的 JSON 格式
        BeanOutputConverter<CompressionResultEnvelope> outputConverter =
                new BeanOutputConverter<>(CompressionResultEnvelope.class);

        // 组装给大模型的 Prompt，明确要求其扮演压缩器角色
        String prompt = """
                你是一个 RAG 上下文压缩器。
                你的任务是针对用户问题，从每个片段中保留最相关的信息。

                规则：
                1. 不要扩写，不要编造
                2. 如果片段与问题无关，keep=false
                3. compressedContent 只保留最关键的信息
                4. 每条 compressedContent 尽量简洁
                5. index 必须对应输入片段中的 index
                6. 只输出 JSON，不要附加解释

                用户问题：
                %s

                输入片段：
                %s

                输出格式：
                %s
                """.formatted(question, buildChunkText(chunks), outputConverter.getFormat());

        try {
            // 调用大语言模型
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
                    
            // 如果返回为空，降级到基础截断
            if (!StringUtils.hasText(response)) {
                return truncateWithoutLlm(chunks);
            }

            // 提取 JSON 并转换为实体对象
            CompressionResultEnvelope envelope = outputConverter.convert(extractJsonObject(response));
            if (envelope == null || envelope.getResults() == null) {
                return truncateWithoutLlm(chunks); // 转换失败时降级
            }

            // 将大模型返回的压缩决策按索引暂存到 Map 中以便快速匹配
            Map<Integer, CompressionDecision> decisionMap = new LinkedHashMap<>();
            for (CompressionDecision decision : envelope.getResults()) {
                if (decision != null && decision.getIndex() != null) {
                    decisionMap.put(decision.getIndex(), decision);
                }
            }

            List<RetrievedChunk> result = new ArrayList<>();
            // 遍历原始片段，结合大模型的决策进行过滤和更新
            for (int i = 0; i < chunks.size(); i++) {
                RetrievedChunk chunk = chunks.get(i);
                CompressionDecision decision = decisionMap.get(i);

                // 如果大模型认为该片段与问题无关（keep=false），则丢弃该片段
                if (decision == null || !Boolean.TRUE.equals(decision.getKeep())) {
                    continue;
                }

                String compressedContent = decision.getCompressedContent();
                // 如果大模型决定保留但没有返回压缩内容
                if (!StringUtils.hasText(compressedContent)) {
                    // 如果配置丢弃空片段，则跳过
                    if (Boolean.TRUE.equals(ragProperties.getCompression().getDropEmptyChunks())) {
                        continue;
                    }
                    // 否则回退为原内容截断
                    compressedContent = truncateToLimit(safeText(chunk.getDocument().getText()));
                }

                // 将压缩后的内容设置回 chunk，并限制单片段最大长度
                chunk.setCompressedContent(truncateToLimit(compressedContent));
                chunk.setSelectedForAnswer(false); // 默认不选中，后续再由限制总长逻辑去开启
                result.add(chunk);
            }

            // 如果压缩后没有任何片段保留，则安全降级到原文本截断
            return result.isEmpty() ? truncateWithoutLlm(chunks) : result;
        } catch (Exception e) {
            // 任何异常（网络请求失败、JSON 解析异常等）均降级处理
            return truncateWithoutLlm(chunks);
        }
    }

    /**
     * 不使用 LLM 的基础降级策略：直接截断片段内容。
     */
    private List<RetrievedChunk> truncateWithoutLlm(List<RetrievedChunk> chunks) {
        List<RetrievedChunk> result = new ArrayList<>();

        for (RetrievedChunk chunk : chunks) {
            // 直接获取原文本并截断到单片段配置的最大长度
            chunk.setCompressedContent(truncateToLimit(safeText(chunk.getDocument().getText())));
            chunk.setSelectedForAnswer(false);
            result.add(chunk);
        }

        return result;
    }

    /**
     * 全局长度限制：按顺序累加保留的片段，当总字符数超过配置上限时丢弃后续片段。
     */
    private List<RetrievedChunk> limitTotalContext(List<RetrievedChunk> chunks) {
        List<RetrievedChunk> result = new ArrayList<>();
        int total = 0;
        int maxTotalChars = safeMaxTotalContextChars();

        for (RetrievedChunk chunk : chunks) {
            chunk.setSelectedForAnswer(false);

            String content = chunk.getCompressedContent();
            if (!StringUtils.hasText(content)) {
                continue;
            }

            int nextLength = total + content.length();
            // 如果加上当前片段长度超出总上限，则终止添加
            if (nextLength > maxTotalChars) {
                break;
            }

            // 标记该片段被最终选中参与问答
            chunk.setSelectedForAnswer(true);
            result.add(chunk);
            total = nextLength;
        }

        return result;
    }

    /**
     * 辅助方法：将传入的文档片段列表格式化为文本，以便嵌入到提供给 LLM 的 Prompt 中。
     */
    private String buildChunkText(List<RetrievedChunk> chunks) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            builder.append("index=").append(i).append("\n"); // 注入索引，供 LLM 返回决策时参考
            builder.append("sourceFileName=")
                    .append(chunk.getDocument().getMetadata().getOrDefault("sourceFileName", "unknown"))
                    .append("\n");
            builder.append("rerankScore=")
                    .append(chunk.getRerankScore())
                    .append("\n");
            builder.append("content=")
                    .append(safeText(chunk.getDocument().getText()))
                    .append("\n\n");
        }

        return builder.toString();
    }

    /**
     * 文本标准化处理：转小写，并将连续的空白字符替换为单个空格，用于去重比较。
     */
    private String normalize(String value) {
        return value.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    /**
     * 截断字符串，使其不超过配置的单个片段最大字符数限制。
     */
    private String truncateToLimit(String value) {
        int maxChars = safeMaxCharsPerChunk();
        return value.length() > maxChars ? value.substring(0, maxChars) : value;
    }

    /**
     * 从大模型的响应字符串中粗略提取 JSON 对象部分（寻找 {} 括号范围）。
     */
    private String extractJsonObject(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response; // 找不到结构时返回原字符串，依靠后续流程抛出异常并降级
    }

    /**
     * 安全地获取单片段的最大字符数配置，如果未配置或不合法则返回 Integer 最大值。
     */
    private int safeMaxCharsPerChunk() {
        Integer value = ragProperties.getCompression().getMaxCharsPerChunk();
        return value == null || value <= 0 ? Integer.MAX_VALUE : value;
    }

    /**
     * 安全地获取全局上下文的最大字符数配置，如果未配置或不合法则返回 Integer 最大值。
     */
    private int safeMaxTotalContextChars() {
        Integer value = ragProperties.getCompression().getMaxTotalContextChars();
        return value == null || value <= 0 ? Integer.MAX_VALUE : value;
    }

    /**
     * 防空指针保护，如果是 null 则返回空字符串。
     */
    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
