package com.qiange.ragdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 第三阶段 RAG 配置绑定类。
 *
 * 主体结构遵循精排与压缩实现文档，
 * 同时保留启动预热所需的兼容字段，避免影响当前本地 Ollama 运行方式。
 */
@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /**
     * 知识库管理相关的配置参数集合
     */
    private KnowledgeBase knowledgeBase = new KnowledgeBase();

    /**
     * 多路检索与排序相关的配置参数集合
     */
    private Retrieval retrieval = new Retrieval();

    /**
     * 精排相关配置
     */
    private Rerank rerank = new Rerank();

    /**
     * 上下文压缩相关配置
     */
    private Compression compression = new Compression();

    /**
     * 评估相关的配置参数集合
     */
    private Evaluation evaluation = new Evaluation();

    @Data
    public static class KnowledgeBase {

        /**
         * 默认分类
         */
        private String defaultCategory = "知识";

        /**
         * 文本分块大小
         */
        private Integer chunkSize = 500;

        /**
         * 分块重叠大小
         */
        private Integer chunkOverlap = 80;

        /**
         * 默认同步目录。
         */
        private String defaultDirectory = "knowledge-base";

        /**
         * 是否递归扫描子目录。
         */
        private Boolean recursiveSync = true;

        /**
         * 向后兼容启动预热逻辑，未显式配置时跟随 defaultDirectory。
         */
        private String bootstrapPath;

        /**
         * 向后兼容启动预热逻辑，未显式配置时跟随 defaultCategory。
         */
        private String bootstrapCategory;

        public String getBootstrapPath() {
            return bootstrapPath != null ? bootstrapPath : defaultDirectory;
        }

        public String getBootstrapCategory() {
            return bootstrapCategory != null ? bootstrapCategory : defaultCategory;
        }
    }

    @Data
    public static class Retrieval {

        /**
         * 向量检索路召回数
         */
        private Integer vectorTopK = 6;

        /**
         * 向量检索相似度阈值
         */
        private Double similarityThreshold = 0.45;

        /**
         * 关键字检索路召回数
         */
        private Integer keywordTopK = 6;

        /**
         * 关键字检索相似度阈值
         */
        private Double trgmSimilarityThreshold = 0.08;

        /**
         * 融合后送给精排服务的候选数。
         */
        private Integer candidateTopK = 8;

        /**
         * RRF 算法的平滑常数 K
         */
        private Integer rrfK = 60;

        /**
         * 是否开启查询词重写
         */
        private Boolean queryRewriteEnabled = true;

        /**
         * 重写生成的查询词数量
         */
        private Integer rewriteQueryCount = 2;
    }

    @Data
    public static class Rerank {

        /**
         * 是否开启精排。
         */
        private Boolean enabled = true;

        /**
         * 精排后保留的高质量片段数。
         */
        private Integer topK = 4;

        /**
         * 精排最低得分阈值。
         */
        private Integer minScore = 55;

        /**
         * 精排时单条片段传给模型的最大字符数。
         */
        private Integer maxContentLengthPerChunk = 350;
    }

    @Data
    public static class Compression {

        /**
         * 是否开启上下文压缩。
         */
        private Boolean enabled = true;

        /**
         * 是否在进入 LLM 前先做规则去重。
         */
        private Boolean deduplicateBeforeLlm = true;

        /**
         * 单条片段压缩后的最大长度。
         */
        private Integer maxCharsPerChunk = 260;

        /**
         * 最终上下文总长度上限。
         */
        private Integer maxTotalContextChars = 1800;

        /**
         * 压缩后为空的片段是否直接丢弃。
         */
        private Boolean dropEmptyChunks = true;
    }

    @Data
    public static class Evaluation {

        /**
         * 评估样本文件路径
         */
        private String caseFile = "retrieval-eval/tutorial-eval-cases.json";
    }
}
