package com.qiange.ragdemo.service;

import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;

/**
 * 知识文档台账服务。
 *
 * 注意这个服务只负责“文档台账”本身，
 * 不负责真正的向量入库。
 * 这样职责会更清晰。
 */
public interface KnowledgeDocumentService {

    /**
     * 根据文档内容的哈希值（唯一标识）查询对应的台账记录。
     * 用于导入前判断文件是否已经存在于知识库中。
     *
     * @param documentId 文档的唯一标识（通常是文件内容的 MD5 值）
     * @return 如果存在返回记录实体，不存在则返回 null
     */
    KnowledgeDocumentEntity getByDocumentId(String documentId);

    /**
     * 创建一条状态为“处理中”（Pending）的文档台账记录。
     * 在文件准备分块入库前调用此方法，占用坑位。
     *
     * @param filePath   导入文件的本地路径
     * @param documentId 文件内容的哈希值（MD5）
     * @param category   文档的分类类别
     * @param title      文档显示标题
     * @return 创建并保存后的实体对象（包含自增主键等）
     */
    KnowledgeDocumentEntity createPendingDocument(String filePath, String documentId, String category, String title);

    /**
     * 将指定的文档记录标记为“导入成功”，并更新切分的总块数。
     * 在向量化存储成功后调用。
     *
     * @param id         数据库记录的自增主键
     * @param chunkCount 文件切分后的总片段数量
     */
    void markImported(Long id, Integer chunkCount);

    /**
     * 将指定的文档记录标记为“导入失败”。
     * 在处理分块或写入向量数据库出现异常时调用。
     *
     * @param id 数据库记录的自增主键
     */
    void markImportFailed(Long id);
}
