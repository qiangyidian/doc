package com.qiange.ragdemo.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.dto.RetrievalEvalCaseResult;
import com.qiange.ragdemo.dto.RetrievalEvaluationResponse;
import com.qiange.ragdemo.service.HybridRetrievalService;
import com.qiange.ragdemo.service.QueryRewriteService;
import com.qiange.ragdemo.service.RetrievalEvaluationService;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 召回评估服务实现。
 *
 * 通过读取预先标注好的“测试样本（Case）”，使用当前的查询重写和混合检索策略，
 * 计算 Hit Rate (命中率) 和 MRR (平均倒数排名) 等关键评估指标。
 */
@Service
@RequiredArgsConstructor
public class RetrievalEvaluationServiceImpl implements RetrievalEvaluationService {

    // 读取评估样本文件路径等配置
    private final RagProperties ragProperties;

    // 查询改写服务（用于模拟完整召回链路）
    private final QueryRewriteService queryRewriteService;

    // 混合检索服务（用于模拟完整召回链路）
    private final HybridRetrievalService hybridRetrievalService;

    // JSON 反序列化工具
    private final ObjectMapper objectMapper;

    /**
     * 运行检索评估并返回统计报告。
     *
     * @return 包含命中率、MRR 等指标和明细的评估结果
     */
    @Override
    public RetrievalEvaluationResponse evaluate() {
        // 1. 从本地 JSON 文件加载测试用例
        List<EvalCase> evalCases = loadCases();
        List<RetrievalEvalCaseResult> caseResults = new ArrayList<>();

        double reciprocalRankSum = 0D;
        int hitCount = 0;

        // 2. 遍历每个测试样本并运行检索
        for (EvalCase evalCase : evalCases) {
            // (a) 将问题重写扩展（遵循系统当前的策略）
            List<String> queries = queryRewriteService.rewriteQueries(evalCase.getQuestion());
            // (b) 进行混合检索召回并融合排序
            List<RetrievedChunk> chunks = hybridRetrievalService.retrieve(queries, evalCase.getCategory());

            // (c) 在截断后的结果列表中，寻找是否存在和样本期望文件同名（表示命中）的片段
            Integer rank = findFirstRank(chunks, evalCase.getExpectedSourceFileName());
            boolean hit = rank != null;

            // (d) 统计命中数和计算单样本的倒数排名
            if (hit) {
                hitCount++;
                reciprocalRankSum += 1D / rank;
            }

            // 记录该样本的单条评估结果
            caseResults.add(RetrievalEvalCaseResult.builder()
                    .question(evalCase.getQuestion())
                    .expectedSourceFileName(evalCase.getExpectedSourceFileName())
                    .hit(hit)
                    .rank(rank)
                    .build());
        }

        // 3. 计算最终的系统级聚合指标
        int totalCases = evalCases.size();
        // 命中率 = Top-K 内成功找到答案所处原文的样本比例
        double hitRate = totalCases == 0 ? 0D : (double) hitCount / totalCases;
        // 平均倒数排名 (MRR) = 所有样本倒数排名的算术平均数，能反映命中结果排位有多靠前
        double mrr = totalCases == 0 ? 0D : reciprocalRankSum / totalCases;

        return RetrievalEvaluationResponse.builder()
                .totalCases(totalCases)
                .hitRate(hitRate)
                .mrr(mrr)
                .caseResults(caseResults)
                .build();
    }

    /**
     * 从配置的文件路径加载 JSON 格式的评估样本。
     */
    private List<EvalCase> loadCases() {
        String caseFile = ragProperties.getEvaluation().getCaseFile();
        if (!StringUtils.hasText(caseFile)) {
            throw new IllegalArgumentException("未配置 rag.evaluation.case-file");
        }

        Path path = Path.of(caseFile);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("评估样本文件不存在：" + path.toAbsolutePath());
        }

        try {
            String json = Files.readString(path);
            // 使用 Jackson 解析 JSON 数组为 List<EvalCase> 对象
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("读取评估样本失败：" + caseFile, e);
        }
    }

    /**
     * 判断期望的文件名是否出现在了检索结果（chunks）列表中。
     * 如果找到了，返回它第一次出现的排位序号 (1-based)。
     * 如果在截断的结果（Top-K）中没找到，则返回 null，计为命中失败 (Miss)。
     */
    private Integer findFirstRank(List<RetrievedChunk> chunks, String expectedSourceFileName) {
        for (int i = 0; i < chunks.size(); i++) {
            // 获取当前检索结果所处文档的“来源文件”元数据
            Object sourceFileName = chunks.get(i).getDocument()
                    .getMetadata()
                    .get(RagConstants.METADATA_SOURCE_FILE_NAME);

            // 对比是否和期望的一致
            if (expectedSourceFileName.equals(String.valueOf(sourceFileName))) {
                // 返回排位，1 表示排在最前面 (Rank 1)
                return i + 1;
            }
        }
        return null;
    }

    /**
     * 内部类：映射评估样本 JSON 中的每个 Case。
     */
    @Data
    private static class EvalCase {

        /**
         * 样本的测试问题
         */
        private String question;

        /**
         * 样本的分类（可选，用于测试分类过滤逻辑）
         */
        private String category;

        /**
         * 这个测试问题正确答案所在的原始文件名（标注好的 Ground Truth）
         */
        private String expectedSourceFileName;

        /**
         * 该样本的作用描述，仅供阅读
         */
        private String description;
    }
}
