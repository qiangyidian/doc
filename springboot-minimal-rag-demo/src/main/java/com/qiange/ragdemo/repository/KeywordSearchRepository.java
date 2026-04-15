package com.qiange.ragdemo.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键词检索仓库。
 *
 * 使用 PostgreSQL 的 pg_trgm 扩展，直接在同一张向量表中发起基于 n-gram（三元字）特征的模糊匹配查询。
 * 这是极简 RAG 系统里弥补纯向量路对特定名词“词不达意”的最轻量级混合方案（无需引入 ES 或别的高级检索组件）。
 */
@Repository
@RequiredArgsConstructor
public class KeywordSearchRepository {

    // Spring 提供的支持命名参数的 JDBC 操作模板
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    // 用于将查询出的 JSON 格式的 metadata 反序列化为 Java Map
    private final ObjectMapper objectMapper;

    // 向量表的名字（如 vector_store_ollama），需要注入以便构建 SQL
    @Value("${spring.ai.vectorstore.pgvector.table-name}")
    private String vectorTableName;

    /**
     * 在数据库中利用 trigram 进行关键字模糊匹配和相似度计算。
     *
     * @param query               检索词
     * @param category            知识分类，如果没有或为空则全量检索
     * @param topK                本次关键字查询想要取回的最高匹配结果数
     * @param similarityThreshold PG trgm 的相似度过滤阈值，低于这个分数的直接剔除（防止把不相干结果混进 RRF）
     * @return 包含文本和元数据的标准 Document 对象列表，并附带了 trigram 评分
     */
    public List<Document> search(String query, String category, int topK, double similarityThreshold) {
        // 构建混合检索 SQL 语句
        String sql = """
                select content,
                       metadata::text as metadata_json,
                       similarity(content, :query) as keyword_score
                from __TABLE__
                where (
                    content ilike '%%' || :query || '%%'
                    or similarity(content, :query) >= :similarityThreshold
                )
                  and (
                    :category is null
                    or metadata ->> 'category' = :category
                  )
                order by
                    case when content ilike '%%' || :query || '%%' then 1 else 0 end desc,
                    similarity(content, :query) desc
                limit :topK
                """.replace("__TABLE__", vectorTableName);

        // 装配命名查询参数
        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("query", query)
                .addValue("category", StringUtils.hasText(category) ? category : null)
                .addValue("similarityThreshold", similarityThreshold)
                .addValue("topK", topK);

        // 执行查询并将每一行 ResultSet 映射为 Spring AI 标准的 Document 对象
        return namedParameterJdbcTemplate.query(sql, parameterSource, (rs, rowNum) -> {
            // 提取文本内容
            String content = rs.getString("content");
            // 提取 JSON 元数据
            String metadataJson = rs.getString("metadata_json");
            // 提取计算出的相关度评分
            Double keywordScore = rs.getDouble("keyword_score");

            Map<String, Object> metadata = new HashMap<>();
            // 解析元数据 JSON 字段
            if (StringUtils.hasText(metadataJson)) {
                try {
                    metadata.putAll(objectMapper.readValue(metadataJson, new TypeReference<>() {
                    }));
                } catch (Exception e) {
                    throw new IllegalStateException("解析关键词检索元数据失败", e);
                }
            }

            // 把 PG 查出的 Trigram 评分存入 Document 的 Metadata 中，便于外部观测
            metadata.put("keywordScore", keywordScore);
            // 构造并返回文档实例
            return new Document(content, metadata);
        });
    }
}
