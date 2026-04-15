package com.qiange.ragdemo.service;

import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.dto.ImportLocalFileRequest;
import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import com.qiange.ragdemo.loader.KnowledgeChunker;
import com.qiange.ragdemo.loader.LocalKnowledgeLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识导入编排服务。
 *
 * 这个类负责把完整导入流程串起来：
 * 1. 读文件
 * 2. 算哈希
 * 3. 建台账
 * 4. 文本分块
 * 5. 写入向量库
 * 6. 更新导入状态
 */
@Service
@RequiredArgsConstructor
public class KnowledgeImportFacade {

    // 知识文档的数据库台账服务
    private final KnowledgeDocumentService knowledgeDocumentService;

    // 本地知识加载器
    private final LocalKnowledgeLoader localKnowledgeLoader;

    // 知识分块器
    private final KnowledgeChunker knowledgeChunker;

    // 向量存储库
    private final VectorStore vectorStore;

    // RAG 配置属性
    private final RagProperties ragProperties;

    /**
     * 导入本地文件至知识库的完整流程。
     *
     * @param request 包含文件路径、分类、标题等信息的导入请求对象
     * @return 成功切分并入库的文本片段数量
     */
    public Integer importLocalFile(ImportLocalFileRequest request) {
        // 1. 确定文档的分类
        String category = StringUtils.hasText(request.getCategory())
                ? request.getCategory()
                : ragProperties.getKnowledgeBase().getDefaultCategory();

        // 2. 加载文档
        Document originalDocument = localKnowledgeLoader.load(
                request.getFilePath(),
                category,
                request.getTitle()
        );

        // 3. 计算文档的 MD5 哈希值
        String fileHash = DigestUtils.md5DigestAsHex(originalDocument.getText().getBytes(StandardCharsets.UTF_8));

        // 4. 去重检查
        KnowledgeDocumentEntity existing = knowledgeDocumentService.getByFileHash(fileHash);
        if (existing != null) {
            throw new IllegalArgumentException("相同内容的文件已经导入过，无需重复导入");
        }

        // 5. 建立台账
        KnowledgeDocumentEntity record = knowledgeDocumentService.createPendingDocument(
                request.getFilePath(),
                fileHash,
                category
        );

        try {
            // 6. 文本分块
            List<Document> chunkDocuments = knowledgeChunker.split(originalDocument);
            if (chunkDocuments.isEmpty()) {
                throw new IllegalArgumentException("切分后没有可入库的有效片段");
            }

            // 7. 写入向量库
            vectorStore.add(chunkDocuments);
            
            // 8. 更新状态（成功）
            knowledgeDocumentService.markImported(
                    record.getId(),
                    "手动导入成功，片段数量：" + chunkDocuments.size()
            );
            return chunkDocuments.size();
        } catch (Exception e) {
            // 9. 异常处理
            knowledgeDocumentService.markImportFailed(record.getId(), e.getMessage());
            throw e;
        }
    }
}
