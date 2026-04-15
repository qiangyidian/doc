package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import com.qiange.ragdemo.mapper.KnowledgeDocumentMapper;
import com.qiange.ragdemo.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

/**
 * 第二阶段文档台账服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public KnowledgeDocumentEntity getByFileHash(String fileHash) {
        return knowledgeDocumentMapper.selectByFileHash(fileHash);
    }

    @Override
    public KnowledgeDocumentEntity getByFilePath(String filePath) {
        return knowledgeDocumentMapper.selectByFilePath(filePath);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentEntity createPendingDocument(String filePath, String fileHash, String category) {
        Path absolutePath = Path.of(filePath).toAbsolutePath();

        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setFileName(absolutePath.getFileName().toString());
        entity.setFilePath(absolutePath.toString());
        entity.setFileHash(fileHash);
        entity.setCategory(category);
        entity.setStatus("PENDING");
        entity.setRemark("首次发现文件，等待同步");

        knowledgeDocumentMapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markBeforeResync(Long id, String fileHash, String remark) {
        knowledgeDocumentMapper.updateBeforeResync(id, fileHash, "PENDING", remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markImported(Long id, String remark) {
        knowledgeDocumentMapper.updateAfterSync(id, "IMPORTED", remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markImportFailed(Long id, String remark) {
        knowledgeDocumentMapper.updateFailed(id, "FAILED", remark);
    }
}
