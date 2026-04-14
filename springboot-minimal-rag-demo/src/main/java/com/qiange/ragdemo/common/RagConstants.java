package com.qiange.ragdemo.common;

/**
 * RAG 元数据（Metadata）键名常量池。
 * 用于统一定义贯穿整个知识生命周期的附加信息键。
 *
 * 为什么这里要专门抽一层常量？
 * 因为知识导入、分块、检索、返回引用片段都会反复用到这些 key。
 * 如果每个地方都手写字符串，很容易出现拼写不一致，最后导致元数据丢失。
 */
public final class RagConstants {

    // 私有化构造方法，防止该常量类被意外实例化
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
}
