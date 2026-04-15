package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.repository.KeywordSearchRepository;
import com.qiange.ragdemo.service.HybridRetrievalService;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

/**
 * 混合检索服务实现。
 *
 * 负责结合向量余弦相似度检索和基于 PG Trigram 的模糊关键字检索，
 * 使用倒数秩融合（Reciprocal Rank Fusion, RRF）算法计算多路召回的总得分并进行重排序。
 */
@Service
@RequiredArgsConstructor
public class HybridRetrievalServiceImpl implements HybridRetrievalService {

    // Spring AI 提供的向量存储接口，主要用于发起向量余弦相似度查询
    private final VectorStore vectorStore;

    // 自定义的持久层仓库，用于执行原生的 PG tgrm (trigram) 模糊匹配查询
    private final KeywordSearchRepository keywordSearchRepository;

    // 统管 RAG 系统里的向量和关键字检索阈值、返回数量 (Top-K) 等超参数
    private final RagProperties ragProperties;

    /**
     * 根据重写后的多条查询词执行混合检索，并使用 RRF 融合排序。
     *
     * @param queries 原始问题和重写产生的一组变体查询词
     * @param category 可选的知识分类过滤条件
     * @return 融合并截断后的知识片段结果集
     */
    @Override
    public List<RetrievedChunk> retrieve(List<String> queries, String category) {
        // 用于合并来自多条查询、多个检索路的结果。Key 是该文档块的唯一标识
        Map<String, RetrievedChunk> mergedChunkMap = new LinkedHashMap<>();

        // 对每一个查询变体，分别发起向量和关键词检索
        for (String query : queries) {
            // 1. 发起向量检索路
            List<Document> vectorDocuments = vectorSearch(query, category);
            // 按照排名累加 RRF 得分
            mergeByRrf(mergedChunkMap, vectorDocuments, RagConstants.RETRIEVAL_SOURCE_VECTOR);

            // 2. 发起关键字检索路（利用 PG 的 pg_trgm 扩展）
            List<Document> keywordDocuments = keywordSearchRepository.search(
                    query,
                    category,
                    ragProperties.getRetrieval().getKeywordTopK(),
                    ragProperties.getRetrieval().getTrgmSimilarityThreshold()
            );
            // 按照排名累加 RRF 得分
            mergeByRrf(mergedChunkMap, keywordDocuments, RagConstants.RETRIEVAL_SOURCE_KEYWORD);
        }

        // 3. 对合并池里所有的知识片段，根据最终累加出来的得分（fusionScore）进行从高到低排序，
        //    并且只返回前 N 个（fusionTopK），避免上下文过长超出大模型窗口。
        return mergedChunkMap.values().stream()
                .sorted(Comparator.comparing(RetrievedChunk::getFusionScore).reversed())
                .limit(ragProperties.getRetrieval().getFusionTopK())
                .toList();
    }

    /**
     * 发起向量检索的独立方法。
     * 封装了对 Spring AI `SearchRequest` 构造器的调用，统一设置了 Top-K 和相似度阈值等过滤条件。
     */
    private List<Document> vectorSearch(String query, String category) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                // 读取 application.yml 中配置的纯向量路要召回的数量
                .topK(ragProperties.getRetrieval().getVectorTopK())
                // 读取应用配置中的向量相似度阈值（值越高代表要求越严苛，越不容易召回相关度低的垃圾内容）
                .similarityThreshold(ragProperties.getRetrieval().getSimilarityThreshold());

        // 处理基于“分类”元数据的过滤条件
        if (StringUtils.hasText(category)) {
            builder.filterExpression("category == '" + escapeFilterValue(category) + "'");
        }

        // 调用向量存储接口获取结果，如未取到返回空列表避免 NPE
        List<Document> documents = vectorStore.similaritySearch(builder.build());
        return documents == null ? new ArrayList<>() : documents;
    }

    /**
     * Reciprocal Rank Fusion (RRF) 的实现。
     * RRF 是一种经典的不依赖于相似度绝对数值（因为向量分和 BM25/Trigram 分不可直接比）的融合排序算法，
     * 它只依赖该文档在每路检索结果中的“排名（Rank）”。
     * 公式为：Score = 1 / (k + rank)
     */
    private void mergeByRrf(Map<String, RetrievedChunk> mergedChunkMap,
                            List<Document> documents,
                            String source) {
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            // 构造当前这个片段在合并池里的唯一标识（防重依据）
            String uniqueKey = buildUniqueKey(document);
            // 根据其在该路结果里的排位索引（i），计算单次得分增量（注意这里的分母加上了平滑常数 RRF_K）
            double deltaScore = 1.0 / (ragProperties.getRetrieval().getRrfK() + i + 1);

            RetrievedChunk existingChunk = mergedChunkMap.get(uniqueKey);
            // 如果这个片段第一次被某种检索路召回，就丢进池子里
            if (existingChunk == null) {
                mergedChunkMap.put(uniqueKey, RetrievedChunk.builder()
                        .uniqueKey(uniqueKey)
                        .document(document)
                        .retrievalSource(source)
                        .fusionScore(deltaScore)
                        .build());
                continue;
            }

            // 如果这个片段已经在池子里（被别的查询词，或另一条检索路召回过了），
            // 则将其历史得分与本次得分进行相加累加，这就是融合！
            existingChunk.setFusionScore(existingChunk.getFusionScore() + deltaScore);
            // 更新片段的来源标记，如果两次来源不同，就会标记为 HYBRID
            //这里是进行手动的进行拼接他的来源
            existingChunk.setRetrievalSource(mergeRetrievalSource(existingChunk.getRetrievalSource(), source));
        }
    }

    /**
     * 生成融合过程中用来判断片段是否为同一个的唯一标识键。
     * 这里优先使用 路径+切块序号，如果没有则降级使用全文本的 MD5 摘要。
     */
    private String buildUniqueKey(Document document) {
        Object sourcePath = document.getMetadata().get(RagConstants.METADATA_SOURCE_PATH);
        Object chunkIndex = document.getMetadata().get(RagConstants.METADATA_CHUNK_INDEX);

        if (sourcePath != null && chunkIndex != null) {
            return sourcePath + "#" + chunkIndex;
        }

        // 兜底方案：计算文本 MD5
        return DigestUtils.md5DigestAsHex(document.getText().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 辅助判断合并检索来源标签。
     * 如果本片段同时被向量检索和关键词检索命中，会被打上 VECTOR+KEYWORD 的标记，
     * 用于前端展示或后期的评估。
     */
    private String mergeRetrievalSource(String current, String incoming) {
        // 如果原本的来源标签是空的（理论上不会），或者与当前路一致，则保持不变
        if (current == null || current.equals(incoming)) {
            return incoming;
        }
        // 如果原来的是向量路，现在进来的是关键字路（或反之），代表被双路都抓到了，使用复合标签
        return RagConstants.RETRIEVAL_SOURCE_HYBRID;
    }

    /**
     * 安全过滤表达式值。
     * 简单处理单引号问题，防止在构建 Spring AI 的 filterExpression 字符串时因标点符号出错。
     */
    private String escapeFilterValue(String value) {
        return value.replace("'", "\\'");
    }
}
