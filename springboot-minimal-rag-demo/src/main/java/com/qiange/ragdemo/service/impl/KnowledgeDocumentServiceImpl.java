package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import com.qiange.ragdemo.mapper.KnowledgeDocumentMapper;
import com.qiange.ragdemo.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

/**
 * 知识库文档台账服务实现。
 * 主要负责维护被导入到知识库中的文件的基础元信息（如文件路径、哈希值、同步状态等），
 * 防止重复导入，并记录每次导入的状态（成功、失败、待处理）。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /**
     * 根据文件的哈希值查询文档台账记录。
     * 通常用于判断文件内容是否发生了变化（如果在路径相同的情况下哈希变了，说明被修改了）。
     *
     * @param fileHash 文件的 SHA-256 或 MD5 哈希值
     * @return 匹配的文档实体，若不存在则返回 null
     */
    @Override
    public KnowledgeDocumentEntity getByFileHash(String fileHash) {
        return knowledgeDocumentMapper.selectByFileHash(fileHash);
    }

    /**
     * 根据文件的绝对路径查询文档台账记录。
     * 通常用于在目录同步扫描时，判断该文件是否已经被系统收录过。
     *
     * @param filePath 文件的绝对路径
     * @return 匹配的文档实体，若不存在则返回 null
     */
    @Override
    public KnowledgeDocumentEntity getByFilePath(String filePath) {
        return knowledgeDocumentMapper.selectByFilePath(filePath);
    }

    /**
     * 创建一条处于“待处理 (PENDING)”状态的全新文档记录。
     * 当系统首次发现某个不在台账中的新文件时调用此方法。
     *
     * @param filePath 文件的绝对路径
     * @param fileHash 文件的哈希值
     * @param category 文件的分类（如果支持按目录分类等）
     * @return 插入数据库后的实体对象（包含生成的 ID）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentEntity createPendingDocument(String filePath, String fileHash, String category) {
        Path absolutePath = Path.of(filePath).toAbsolutePath();

        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setFileName(absolutePath.getFileName().toString());
        entity.setFilePath(absolutePath.toString());
        entity.setFileHash(fileHash);
        entity.setCategory(category);
        entity.setStatus("PENDING"); // 初始状态为待同步
        entity.setRemark("首次发现文件，等待同步");

        knowledgeDocumentMapper.insert(entity);
        return entity;
    }

    /**
     * 在重新同步（或更新）文件前，将现有的记录状态标记为“待处理 (PENDING)”。
     * 适用于文件内容发生了修改，需要重新读取并向量化的情况。
     *
     * @param id 文档在数据库中的主键 ID
     * @param fileHash 最新的文件哈希值
     * @param remark 备注信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markBeforeResync(Long id, String fileHash, String remark) {
        knowledgeDocumentMapper.updateBeforeResync(id, fileHash, "PENDING", remark);
    }

    /**
     * 当文件成功被读取、切分并存入向量数据库后，将该文档的状态标记为“已导入 (IMPORTED)”。
     *
     * @param id 文档在数据库中的主键 ID
     * @param remark 备注信息（如成功处理的 chunk 数量等）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markImported(Long id, String remark) {
        knowledgeDocumentMapper.updateAfterSync(id, "IMPORTED", remark);
    }

    /**
     * 当文件在读取或向量化过程中发生错误时，将该文档状态标记为“失败 (FAILED)”。
     *
     * @param id 文档在数据库中的主键 ID
     * @param remark 失败的异常信息或原因描述
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markImportFailed(Long id, String remark) {
        knowledgeDocumentMapper.updateFailed(id, "FAILED", remark);
    }
}
