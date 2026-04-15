package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.dto.SyncKnowledgeDirectoryRequest;
import com.qiange.ragdemo.dto.SyncKnowledgeDirectoryResponse;
import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import com.qiange.ragdemo.loader.KnowledgeChunker;
import com.qiange.ragdemo.loader.LocalKnowledgeLoader;
import com.qiange.ragdemo.repository.VectorStoreCleanupRepository;
import com.qiange.ragdemo.service.KnowledgeDirectorySyncService;
import com.qiange.ragdemo.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 知识目录同步服务实现。
 *
 * 负责扫描指定目录，处理文件的增量同步和全量同步。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDirectorySyncServiceImpl implements KnowledgeDirectorySyncService {

    // RAG 相关配置属性
    private final RagProperties ragProperties;

    // 知识文档台账服务
    private final KnowledgeDocumentService knowledgeDocumentService;

    // 本地文件加载器
    private final LocalKnowledgeLoader localKnowledgeLoader;

    // 知识分块器
    private final KnowledgeChunker knowledgeChunker;

    // 用于清理旧向量的自定义仓库
    private final VectorStoreCleanupRepository vectorStoreCleanupRepository;

    // Spring AI 向量存储接口
    private final VectorStore vectorStore;

    /**
     * 执行指定目录的文件扫描和同步。
     *
     * @param request 包含目录路径、分类和是否递归扫描等参数的请求
     * @return 包含扫描、导入、更新、跳过和失败统计信息的响应体
     */
    @Override
    public SyncKnowledgeDirectoryResponse syncDirectory(SyncKnowledgeDirectoryRequest request) {
        // 确定同步目录，如果没有指定则使用配置文件中的默认值
        String directoryPath = StringUtils.hasText(request.getDirectoryPath())
                ? request.getDirectoryPath()
                : ragProperties.getKnowledgeBase().getDefaultDirectory();

        // 确定文件分类，如果没有指定则使用配置文件中的默认值
        String category = StringUtils.hasText(request.getCategory())
                ? request.getCategory()
                : ragProperties.getKnowledgeBase().getDefaultCategory();

        // 确定是否递归扫描子目录，如果没有指定则使用配置文件中的默认值
        boolean recursive = request.getRecursive() != null
                ? request.getRecursive()
                : Boolean.TRUE.equals(ragProperties.getKnowledgeBase().getRecursiveSync());

        // 验证目录是否存在且是一个目录
        Path directory = Path.of(directoryPath).toAbsolutePath();
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("知识目录不存在：" + directory);
        }

        // 扫描目录并获取所有支持的文件列表
        List<Path> files = listKnowledgeFiles(directory, recursive);

        // 初始化统计变量
        int importedFiles = 0;
        int updatedFiles = 0;
        int skippedFiles = 0;
        int failedFiles = 0;

        // 遍历所有文件并执行同步操作
        for (Path file : files) {
            try {
                SyncAction action = syncSingleFile(file, category);
                switch (action) {
                    case IMPORTED -> importedFiles++;
                    case UPDATED -> updatedFiles++;
                    case SKIPPED -> skippedFiles++;
                }
            } catch (Exception e) {
                // 记录失败的文件数，但不中断整个同步过程
                failedFiles++;
            }
        }

        // 构建并返回同步结果的统计响应
        return SyncKnowledgeDirectoryResponse.builder()
                .scannedFiles(files.size())
                .importedFiles(importedFiles)
                .updatedFiles(updatedFiles)
                .skippedFiles(skippedFiles)
                .failedFiles(failedFiles)
                .build();
    }

    /**
     * 同步单个文件，处理首次导入、跳过未变更、以及更新已变更文件的逻辑。
     *
     * @param file 文件的物理路径
     * @param category 文件的分类
     * @return 表示文件同步结果状态的枚举
     */
    private SyncAction syncSingleFile(Path file, String category) {
        // 1. 加载并解析本地文件
        Document originalDocument = localKnowledgeLoader.load(file.toString(), category, null);
        // 获取归一化的绝对路径作为该文档台账的唯一标识
        String normalizedPath = file.toAbsolutePath().toString();
        // 计算文本内容的 MD5 哈希，用于判断文件内容是否发生了变化
        String fileHash = DigestUtils.md5DigestAsHex(originalDocument.getText().getBytes(StandardCharsets.UTF_8));

        // 2. 根据文件路径查询其是否已经在数据库台账中存在
        KnowledgeDocumentEntity existing = knowledgeDocumentService.getByFilePath(normalizedPath);
        
        // 分支 A: 文件首次被发现，准备新导入
        if (existing == null) {
            KnowledgeDocumentEntity created = knowledgeDocumentService.createPendingDocument(
                    normalizedPath,
                    fileHash,
                    category
            );

            try {
                // 执行分块和向量入库
                List<Document> chunks = knowledgeChunker.split(originalDocument);
                vectorStore.add(chunks);
                // 成功后标记状态并备注片段数
                knowledgeDocumentService.markImported(created.getId(), "首次同步成功，片段数量：" + chunks.size());
                return SyncAction.IMPORTED;
            } catch (Exception e) {
                // 失败则标记状态，并让上层捕捉到记录 failedFiles
                knowledgeDocumentService.markImportFailed(created.getId(), e.getMessage());
                throw e;
            }
        }

        // 分支 B: 台账中已有此文件，比较内容哈希判断是否需要更新
        // 如果哈希相同，说明文件没改过，直接跳过以节省算力
        if (fileHash.equals(existing.getFileHash())) {
            return SyncAction.SKIPPED;
        }

        // 分支 C: 台账已有但内容哈希变了，触发增量重同步逻辑
        knowledgeDocumentService.markBeforeResync(
                existing.getId(),
                fileHash,
                "检测到文件内容变更，准备重新同步"
        );

        try {
            // C1. 先调用清理服务，将该旧文件路径在向量库表里残留的旧 chunk 向量给删掉
            vectorStoreCleanupRepository.deleteBySourcePath(normalizedPath);
            // C2. 重新进行切分
            List<Document> chunks = knowledgeChunker.split(originalDocument);
            // C3. 将最新的 chunk 及生成的向量存入底层库
            vectorStore.add(chunks);
            // 标记状态为导入成功
            knowledgeDocumentService.markImported(existing.getId(), "增量同步成功，片段数量：" + chunks.size());
            return SyncAction.UPDATED;
        } catch (Exception e) {
            knowledgeDocumentService.markImportFailed(existing.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 辅助方法：扫描并过滤指定目录及其子目录，提取支持的知识文件。
     */
    private List<Path> listKnowledgeFiles(Path directory, boolean recursive) {
        try (Stream<Path> stream = recursive ? Files.walk(directory) : Files.list(directory)) {
            return stream.filter(Files::isRegularFile)          // 只取实体文件
                    .filter(this::isKnowledgeFile)              // 过滤指定后缀
                    .sorted()                                   // 排序，保证每次执行的确定性
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("扫描知识目录失败：" + directory, e);
        }
    }

    /**
     * 过滤：目前只支持同步 .md 和 .txt 结尾的文件。
     */
    private boolean isKnowledgeFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".md") || fileName.endsWith(".txt");
    }

    /**
     * 描述单文件同步结果的内部枚举。
     */
    private enum SyncAction {
        // 新建并导入
        IMPORTED,
        // 内容发生改变重新更新
        UPDATED,
        // 内容无变化被跳过
        SKIPPED
    }
}
