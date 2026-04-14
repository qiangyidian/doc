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
 * 这个类不是简单 CRUD，它负责把完整导入流程串起来：
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

    // 知识文档的数据库台账服务，负责记录文档导入的状态、哈希值等信息
    private final KnowledgeDocumentService knowledgeDocumentService;

    // 本地知识加载器，负责将本地文件读取并转换为大模型可识别的 Document 对象
    private final LocalKnowledgeLoader localKnowledgeLoader;

    // 知识分块器，负责将长文本按照指定规则切分成小块的文本片段
    private final KnowledgeChunker knowledgeChunker;

    // 向量存储库，负责与底层的向量数据库（如 Chroma、Milvus、PGVector 等）交互，存储分块及其向量表示
    private final VectorStore vectorStore;

    // RAG 配置属性，提供如默认分类等配置信息
    private final RagProperties ragProperties;

    /**
     * 导入本地文件至知识库的完整流程。
     *
     * @param request 包含文件路径、分类、标题等信息的导入请求对象
     * @return 成功切分并入库的文本片段（Chunk）数量
     */
    public Integer importLocalFile(ImportLocalFileRequest request) {
        // 1. 确定文档的分类：如果请求中指定了分类则使用，否则使用配置中的默认分类
        String category = StringUtils.hasText(request.getCategory())
                ? request.getCategory()
                : ragProperties.getKnowledgeBase().getDefaultCategory();

        // 2. 加载文档：从指定的本地文件路径读取内容，并生成原始的 Document 对象（包含文本内容和初始元数据）
        Document originalDocument = localKnowledgeLoader.load(
                request.getFilePath(),
                category,
                request.getTitle()
        );

        // 3. 计算文档的 MD5 哈希值，用于唯一标识该文档内容，防止重复导入
        String fileHash = DigestUtils.md5DigestAsHex(originalDocument.getText().getBytes(StandardCharsets.UTF_8));

        // 4. 去重检查：通过哈希值查询数据库台账，看是否已经存在相同内容的文件
        KnowledgeDocumentEntity existing = knowledgeDocumentService.getByDocumentId(fileHash);
        if (existing != null) {
            throw new IllegalArgumentException("相同内容的文件已经导入过，无需重复导入");
        }

        // 5. 建立台账：在数据库中创建一条导入记录，初始状态通常为“处理中”（Pending）
        KnowledgeDocumentEntity record = knowledgeDocumentService.createPendingDocument(
                request.getFilePath(),
                fileHash,
                category,
                request.getTitle()
        );

        try {
            // 6. 文本分块：将读取到的完整文档拆分成多个具有重叠的小片段，以适应大模型的上下文窗口
            List<Document> chunkDocuments = knowledgeChunker.split(originalDocument);

            // 检查切分结果，如果没有切分出任何有效的片段则抛出异常
            if (chunkDocuments.isEmpty()) {
                throw new IllegalArgumentException("切分后没有可入库的有效片段");
            }

            // 7. 写入向量库：将切分后的多个文本片段保存到向量数据库中。这一步会自动调用 Embedding 模型生成向量
            vectorStore.add(chunkDocuments);

            // 8. 更新状态（成功）：如果向量库写入成功，更新数据库台账中的状态为“导入成功”，并记录切分出的片段总数
            knowledgeDocumentService.markImported(
                    record.getId(),
                    chunkDocuments.size()
            );

            // 9. 返回导入的片段数量，供调用方参考
            return chunkDocuments.size();
        } catch (Exception e) {
            // 10. 异常处理：如果在切分或写入向量库的过程中发生异常，则将数据库台账中的状态更新为“导入失败”
            knowledgeDocumentService.markImportFailed(record.getId());
            // 继续向上抛出异常，让上层能够感知到失败
            throw e;
        }
    }
}
