package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import com.qiange.ragdemo.mapper.KnowledgeDocumentMapper;
import com.qiange.ragdemo.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

/**
 * 知识文档台账服务实现。
 * 主要负责记录上传文档的元信息及处理状态，以便进行查重和管理。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    // 依赖注入数据库映射接口，执行底层 SQL 操作
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /**
     * 根据文档的唯一标识（通常是文件内容的MD5哈希值）查询文档记录。
     * 此方法主要用于在导入新文档前进行查重，避免重复处理相同内容的文档。
     *
     * @param documentId 文档的唯一标识（MD5哈希值）
     * @return 匹配的文档实体对象，如果不存在则返回 null
     */
    @Override
    public KnowledgeDocumentEntity getByDocumentId(String documentId) {
        return knowledgeDocumentMapper.selectByDocumentId(documentId);
    }

    /**
     * 创建一条初始状态为“PENDING”（处理中）的文档记录。
     * 这个步骤相当于在数据库中“占位”，记录下文档的基本信息，随后进行分块和向量化处理。
     *
     * @param filePath 原始文件的本地路径
     * @param documentId 根据文件内容计算出的MD5哈希值，作为文档的唯一标识
     * @param category 文档所属的分类，用于检索时过滤
     * @param title 用户指定的文档标题，如果未提供，则默认使用文件名
     * @return 插入数据库后包含生成主键的文档实体对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，如果保存记录失败则回滚
    public KnowledgeDocumentEntity createPendingDocument(String filePath, String documentId, String category, String title) {
        // 将相对路径转换为绝对路径，方便后续通过 NIO 处理
        Path sourcePath = Path.of(filePath).toAbsolutePath();
        // 提取文件名
        String fileName = sourcePath.getFileName().toString();

        // 初始化文档记录实体
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setDocId(documentId);
        // 如果提供了标题则使用提供的值，否则退化使用文件名作为标题
        entity.setTitle(StringUtils.hasText(title) ? title : fileName);
        entity.setFileName(fileName);
        entity.setCategory(category);
        entity.setSourcePath(sourcePath.toString());
        // 初始状态设为 PENDING (处理中)
        entity.setStatus("PENDING");
        // 分块尚未进行，因此分块数量初始化为 0
        entity.setChunkCount(0);

        // 调用 Mapper 将实体对象持久化到数据库
        knowledgeDocumentMapper.insert(entity);
        return entity;
    }

    /**
     * 标记文档导入成功。
     * 在文档成功分块并入库到向量数据库后调用此方法，将文档状态更新为“IMPORTED”（已导入），
     * 并记录最终生成的分块数量。
     *
     * @param id 数据库中该文档记录的主键ID
     * @param chunkCount 文档被切分后的总块数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markImported(Long id, Integer chunkCount) {
        // 更新指定 ID 的文档记录的状态和分块数
        knowledgeDocumentMapper.updateStatusAndChunkCount(id, "IMPORTED", chunkCount);
    }

    /**
     * 标记文档导入失败。
     * 如果在分块或向量化存储过程中发生异常，调用此方法将文档状态更新为“FAILED”（失败），
     * 以便后续排查或重试。
     *
     * @param id 数据库中该文档记录的主键ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markImportFailed(Long id) {
        // 更新指定 ID 的文档记录的状态为 FAILED，分块数重置为 0
        knowledgeDocumentMapper.updateStatusAndChunkCount(id, "FAILED", 0);
    }
}