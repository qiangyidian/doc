package com.qiange.ragdemo.service;

import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;

/**
 * 第二阶段文档台账服务。
 *
 * 负责记录上传文档的元信息及处理状态，以便进行查重和管理。
 */
public interface KnowledgeDocumentService {

    /**
     * 根据文件哈希获取文档实体
     *
     * @param fileHash 文件哈希
     * @return 文档实体
     */
    KnowledgeDocumentEntity getByFileHash(String fileHash);

    /**
     * 根据文件路径获取文档实体
     *
     * @param filePath 文件路径
     * @return 文档实体
     */
    KnowledgeDocumentEntity getByFilePath(String filePath);

    /**
     * 创建一个待处理的文档台账记录
     *
     * @param filePath 文件路径
     * @param fileHash 文件哈希
     * @param category 分类
     * @return 文档实体
     */
    KnowledgeDocumentEntity createPendingDocument(String filePath, String fileHash, String category);

    /**
     * 在重新同步前标记状态并更新哈希
     *
     * @param id 文档 ID
     * @param fileHash 新的文件哈希
     * @param remark 备注信息
     */
    void markBeforeResync(Long id, String fileHash, String remark);

    /**
     * 标记导入成功
     *
     * @param id 文档 ID
     * @param remark 备注信息
     */
    void markImported(Long id, String remark);

    /**
     * 标记导入失败
     *
     * @param id 文档 ID
     * @param remark 备注信息
     */
    void markImportFailed(Long id, String remark);
}
