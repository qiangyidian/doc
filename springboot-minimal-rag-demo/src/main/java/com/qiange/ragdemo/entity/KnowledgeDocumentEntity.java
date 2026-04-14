package com.qiange.ragdemo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识文档台账实体类，映射数据库中的 `knowledge_document` 表。
 *
 * 注意：
 * 这个类对应的表不是实际存储文本分块和高维向量的“向量表（Vector Table）”。
 * 它的职责更像是建立一个“导入任务台账”和“知识文件目录”。
 *
 * 为什么即便是最小规模的 RAG 项目也强烈建议保留这张表？
 * 因为它可以非常优雅地解决业务层面的两个关键痛点：
 * 1. 防重与溯源：明确知道哪些文件已经被系统处理过，并且记录其内容的哈希值，防止重复处理相同文件。
 * 2. 状态跟踪：可以记录文件是被成功拆分和入库，还是在某个环节发生异常。导入失败时能够快速定位到问题文件。
 */
@Data // 自动生成字段的 get/set, toString 等方法
public class KnowledgeDocumentEntity {

    // 数据库自增主键，作为内部的唯一标识
    private Long id;

    // 文档内容的哈希摘要（如 MD5）。核心用作查重逻辑：不同文件但内容相同，哈希值一致，避免浪费 Tokens
    private String docId;

    // 文档展示标题。可以由用户在上传时指定，如果没有指定通常会使用文件名作为兜底
    private String title;

    // 原始文件名，用于追溯和前端显示。例如：01-rag-intro.md
    private String fileName;

    // 文档的分类标签（Category）。可作为 RAG 检索阶段强大的 Metadata 过滤维度
    private String category;

    // 原始文件在服务器或者存储介质上的完整路径。方便在需要重新处理或提供源文件下载时定位
    private String sourcePath;

    // 当前文件的导入处理状态。通常包含：PENDING(处理中/排队)、IMPORTED(导入成功)、FAILED(失败)
    private String status;

    // 文件经过 Chunker 处理后，最终拆分出来的文本片段（Document 块）总数。用于统计和核对
    private Integer chunkCount;

    // 记录在数据库的创建时间。由数据库默认生成，表示导入任务发起的时刻
    private LocalDateTime createdAt;

    // 记录在数据库的更新时间。当状态从 PENDING 变为 IMPORTED 或 FAILED 时会更新
    private LocalDateTime updatedAt;
}
