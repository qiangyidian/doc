package com.qiange.ragdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 把 application.yml 中的 rag.* 前缀的自定义配置项映射到强类型的 Java 对象中。
 * 用于集中管理如分块大小、检索 Top-K 等核心的 RAG 超参数。
 *
 * 这么做的价值很大：
 * 1. 配置有类型约束，提供代码补全功能，避免了魔法字符串到处飞
 * 2. 后续调优 chunk-size、top-k 等关键参数时，直接修改 yml 即可，不用重新编译业务代码
 * 3. 非常符合教程里“参数要统一管理且可调”的工程化思路
 */
@Data // 自动生成 Getter/Setter 等
@ConfigurationProperties(prefix = "rag") // 标识这是一个配置属性类，将绑定 yml 中以 'rag' 开头的配置
public class RagProperties {

    // 知识库相关的配置参数集合，默认实例化以防空指针
    private KnowledgeBase knowledgeBase = new KnowledgeBase();

    // 向量检索阶段相关的配置参数集合，默认实例化
    private Retrieval retrieval = new Retrieval();

    /**
     * 内部类：针对知识导入与分块环节的配置项。
     */
    @Data
    public static class KnowledgeBase {

        /**
         * 默认的知识分类标签。
         * 如果用户上传文件时不指定分类，就使用该默认值。
         * 最小版项目里我们先用这个字段做最基础的知识隔离。
         */
        private String defaultCategory = "知识";

        /**
         * 文本分块大小（Chunk Size），通常指字符数。
         * 长文档会被切分成不超过这个长度的许多小块，以便喂给语言模型。
         * 这里是一个“近似字符级”配置，用于帮助初学者理解分块原理。
         */
        private Integer chunkSize = 500;

        /**
         * 相邻两个分块之间的重叠字符长度（Chunk Overlap）。
         * 这个参数的意义是减少切块时造成的上下文语义断裂问题。
         * （比如一句话正好被切分成了两半，重叠能够保证两块都包含这句话的完整意思）
         */
        private Integer chunkOverlap = 80;

        /**
         * 项目启动时，自动扫描并导入的预置知识库目录名称（相对于项目根路径或 classpath）。
         */
        private String bootstrapPath = "knowledge-base";

        /**
         * 项目启动时预热导入的文件所默认归属的分类标签。
         */
        private String bootstrapCategory = "知识";
    }

    /**
     * 内部类：针对大模型问答时的检索环节配置项。
     */
    @Data
    public static class Retrieval {

        /**
         * 一次检索操作召回多少个知识片段（Top-K）。
         * 这决定了最后提供给大模型作为“上下文”的参考材料数量。
         * 太少可能找不到答案，太多可能超出模型的 Token 限制或引入无关噪音。
         */
        private Integer topK = 5;

        /**
         * 向量余弦相似度阈值（通常在 0 ~ 1 之间）。
         * 只有当知识库片段与用户提问的相似度大于这个值时，才会被取回使用。
         * 阈值越高，召回的结果越“精确严格”，但也容易漏掉；
         * 阈值越低，召回的结果越“宽泛宽松”，但也容易混入不相关的内容。
         */
        private Double similarityThreshold = 0.55;
    }
}
