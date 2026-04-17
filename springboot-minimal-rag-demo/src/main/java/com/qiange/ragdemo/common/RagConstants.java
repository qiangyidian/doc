package com.qiange.ragdemo.common;

/**
 * RAG 元数据和检索流水线常量。
 */
public final class RagConstants {

    private RagConstants() {
    }

    /**
     * 元数据键：文档的原始文件名。
     */
    public static final String METADATA_SOURCE_FILE_NAME = "sourceFileName";

    /**
     * 元数据键：文档的原始路径（如本地绝对路径）。
     */
    public static final String METADATA_SOURCE_PATH = "sourcePath";

    /**
     * 元数据键：文档所属分类，可用于检索时过滤。
     */
    public static final String METADATA_CATEGORY = "category";

    /**
     * 元数据键：文档的展示标题。
     */
    public static final String METADATA_TITLE = "title";

    /**
     * 元数据键：分块后，当前文档片段在源文档中的块索引。
     */
    public static final String METADATA_CHUNK_INDEX = "chunkIndex";

    /**
     * 元数据键：分块后，当前文档片段在源文档全文中的起始字符偏移量。
     */
    public static final String METADATA_START_OFFSET = "startOffset";

    /**
     * 元数据键：分块后，当前文档片段在源文档全文中的结束字符偏移量。
     */
    public static final String METADATA_END_OFFSET = "endOffset";

    /**
     * 检索来源：纯向量检索
     */
    public static final String RETRIEVAL_SOURCE_VECTOR = "VECTOR";

    /**
     * 检索来源：纯关键字（如 BM25）检索
     */
    public static final String RETRIEVAL_SOURCE_KEYWORD = "KEYWORD";

    /**
     * 检索来源：混合（向量 + 关键字）检索
     */
    public static final String RETRIEVAL_SOURCE_HYBRID = "VECTOR+KEYWORD";

    /**
     * 片段已进入最终答案上下文。
     */
    public static final String PIPELINE_SELECTED = "SELECTED";

    /**
     * 片段在流水线中被淘汰。
     */
    public static final String PIPELINE_DROPPED = "DROPPED";
}
