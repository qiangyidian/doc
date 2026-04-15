package com.qiange.ragdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 第二阶段 RAG 配置绑定类。
 *
 * 主体结构遵循增强实现文档，
 * 仅额外保留了少量兼容字段，避免打断当前项目已经跑通的本地预热链路。
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
         * 融合后最终返回的召回数
         */
        private Integer fusionTopK = 6;

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
    public static class Evaluation {

        /**
         * 评估样本文件路径
         */
        private String caseFile = "retrieval-eval/tutorial-eval-cases.json";
    }
}
