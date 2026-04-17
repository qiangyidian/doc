package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.service.RerankService;
import com.qiange.ragdemo.service.model.RerankDecision;
import com.qiange.ragdemo.service.model.RerankResultEnvelope;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三阶段精排服务实现。
 * <p>
 * 该服务负责接收初步检索到的候选文本片段，并利用大语言模型（LLM）对其进行重新排序和筛选，
 * 以找出与用户问题最相关的片段，用于后续的答案生成。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RerankServiceImpl implements RerankService {

    private final ChatClient chatClient;

    private final RagProperties ragProperties;

    /**
     * 对候选文本片段进行精排。
     *
     * @param question        用户提出的问题。
     * @param candidateChunks 初步检索召回的候选文本片段列表。
     * @return 经过精排和筛选后的文本片段列表。
     */
    @Override
    public List<RetrievedChunk> rerank(String question, List<RetrievedChunk> candidateChunks) {
        // 如果没有候选片段，直接返回空列表
        if (candidateChunks == null || candidateChunks.isEmpty()) {
            return List.of();
        }

        // 重置所有片段的选择状态为未选中
        resetSelection(candidateChunks);

        // 检查配置是否启用了精排功能
        if (!Boolean.TRUE.equals(ragProperties.getRerank().getEnabled())) {
            // 如果未启用，则执行降级策略（基于融合分数排序）
            return fallbackByFusion(candidateChunks);
        }

        // 创建一个输出转换器，用于将LLM返回的JSON字符串转换为RerankResultEnvelope对象
        BeanOutputConverter<RerankResultEnvelope> outputConverter =
                new BeanOutputConverter<>(RerankResultEnvelope.class);

        // 构建发送给LLM的提示（Prompt）
        String prompt = """
                你是一个 RAG 精排器。
                请根据用户问题，对每个候选片段打分并决定是否保留。

                规则：
                1. score 范围 0 到 100
                2. keep=true 表示建议保留
                3. 高分片段应能直接支持回答用户问题
                4. 结果中的 index 必须对应候选片段中的 index
                5. 只输出 JSON，不要附加解释

                用户问题：
                %s

                候选片段：
                %s

                输出格式：
                %s
                """.formatted(question, buildCandidateText(candidateChunks), outputConverter.getFormat());

        try {
            // 调用LLM进行推理
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            // 如果LLM返回内容为空，则执行降级策略
            if (!StringUtils.hasText(response)) {
                return fallbackByFusion(candidateChunks);
            }

            // 解析LLM的响应，将其转换为RerankResultEnvelope对象
            RerankResultEnvelope envelope = outputConverter.convert(extractJsonObject(response));
            Map<Integer, RerankDecision> decisionMap = new HashMap<>();
            if (envelope != null && envelope.getResults() != null) {
                // 将LLM的决策（分数和是否保留）存入Map，方便按索引查找
                for (RerankDecision decision : envelope.getResults()) {
                    if (decision != null && decision.getIndex() != null) {
                        decisionMap.put(decision.getIndex(), decision);
                    }
                }
            }

            // 根据LLM的决策筛选和处理候选片段
            List<RetrievedChunk> rerankedChunks = new ArrayList<>();
            for (int i = 0; i < candidateChunks.size(); i++) {
                RetrievedChunk chunk = candidateChunks.get(i);
                RerankDecision decision = decisionMap.get(i);

                // 如果LLM建议不保留，则跳过
                if (decision == null || !Boolean.TRUE.equals(decision.getKeep())) {
                    continue;
                }

                // 获取LLM给出的分数，如果分数低于配置的最小阈值，则跳过
                double score = decision.getScore() == null ? 0D : decision.getScore();
                if (score < ragProperties.getRerank().getMinScore()) {
                    continue;
                }

                // 为通过筛选的片段设置精排分数，并将其加入结果列表
                chunk.setRerankScore(score);
                chunk.setSelectedForAnswer(false); // 先重置为false，后续排序后会再标记
                rerankedChunks.add(chunk);
            }

            // 如果经过LLM筛选后没有剩余的片段，则执行降级策略
            if (rerankedChunks.isEmpty()) {
                return fallbackByFusion(candidateChunks);
            }

            // 对筛选后的片段按精排分数进行降序排序，并取前K个
            return rerankedChunks.stream()
                    .sorted(Comparator.comparing(RerankServiceImpl::safeRerankScore).reversed())
                    .limit(safeTopK())
                    .peek(chunk -> chunk.setSelectedForAnswer(true)) // 标记最终选中的片段
                    .toList();
        } catch (Exception e) {
            // 如果在调用LLM或处理结果过程中发生任何异常，执行降级策略
            return fallbackByFusion(candidateChunks);
        }
    }

    /**
     * 降级处理逻辑，当精排失败或未启用时调用。
     * 该方法会根据候选片段的融合分数（fusionScore）进行排序和选择。
     *
     * @param candidateChunks 候选文本片段列表。
     * @return 根据融合分数排序后的文本片段列表。
     */
    private List<RetrievedChunk> fallbackByFusion(List<RetrievedChunk> candidateChunks) {
        return candidateChunks.stream()
                .sorted(Comparator.comparing(RerankServiceImpl::safeFusionScore).reversed()) // 按融合分数降序排序
                .limit(safeTopK()) // 取前K个
                .peek(chunk -> {
                    // 在降级模式下，将融合分数作为精排分数
                    chunk.setRerankScore(safeFusionScore(chunk));
                    // 标记为最终选中的片段
                    chunk.setSelectedForAnswer(true);
                })
                .toList();
    }

    /**
     * 构建用于LLM Prompt的候选片段文本。
     *
     * @param candidateChunks 候选文本片段列表。
     * @return 格式化后的字符串，包含所有候选片段的信息。
     */
    private String buildCandidateText(List<RetrievedChunk> candidateChunks) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < candidateChunks.size(); i++) {
            RetrievedChunk chunk = candidateChunks.get(i);
            String content = chunk.getDocument().getText();
            if (!StringUtils.hasText(content)) {
                content = "";
            }
            // 根据配置截断过长的文本内容，防止超出LLM的上下文长度限制
            if (content.length() > ragProperties.getRerank().getMaxContentLengthPerChunk()) {
                content = content.substring(0, ragProperties.getRerank().getMaxContentLengthPerChunk());
            }

            // 拼接每个片段的信息，包括索引、元数据和内容
            builder.append("index=").append(i).append("\n");
            builder.append("sourceFileName=")
                    .append(chunk.getDocument().getMetadata().getOrDefault("sourceFileName", "unknown"))
                    .append("\n");
            builder.append("retrievalSource=")
                    .append(chunk.getRetrievalSource())
                    .append("\n");
            builder.append("fusionScore=")
                    .append(chunk.getFusionScore())
                    .append("\n");
            builder.append("content=")
                    .append(content)
                    .append("\n\n");
        }

        return builder.toString();
    }

    /**
     * 重置所有候选片段的“是否被选中用于回答”状态。
     *
     * @param candidateChunks 候选文本片段列表。
     */
    private void resetSelection(List<RetrievedChunk> candidateChunks) {
        for (RetrievedChunk chunk : candidateChunks) {
            chunk.setSelectedForAnswer(false);
        }
    }

    /**
     * 从LLM的响应字符串中提取出JSON对象部分。
     * LLM的返回可能包含Markdown代码块标记（```json ... ```）或其他解释性文本。
     *
     * @param response LLM返回的原始字符串。
     * @return 提取出的JSON字符串。
     */
    private String extractJsonObject(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * 安全地获取配置的topK值。
     *
     * @return topK值，如果未配置或无效，则返回一个极大值。
     */
    private int safeTopK() {
        Integer topK = ragProperties.getRerank().getTopK();
        return topK == null || topK <= 0 ? Integer.MAX_VALUE : topK;
    }

    /**
     * 安全地获取片段的融合分数。
     *
     * @param chunk 文本片段。
     * @return 融合分数，如果为null则返回0。
     */
    private static Double safeFusionScore(RetrievedChunk chunk) {
        return chunk.getFusionScore() == null ? 0D : chunk.getFusionScore();
    }

    /**
     * 安全地获取片段的精排分数。
     *
     * @param chunk 文本片段。
     * @return 精排分数，如果为null则返回0。
     */
    private static Double safeRerankScore(RetrievedChunk chunk) {
        return chunk.getRerankScore() == null ? 0D : chunk.getRerankScore();
    }
}
