package com.qiange.ragdemo.service;

import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.dto.SyncKnowledgeDirectoryRequest;
import com.qiange.ragdemo.dto.SyncKnowledgeDirectoryResponse;
import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import com.qiange.ragdemo.loader.KnowledgeChunker;
import com.qiange.ragdemo.loader.LocalKnowledgeLoader;
import com.qiange.ragdemo.repository.VectorStoreCleanupRepository;
import com.qiange.ragdemo.service.impl.KnowledgeDirectorySyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeDirectorySyncServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void syncDirectoryShouldCountImportedUpdatedAndSkippedFiles() throws IOException {
        Path importedFile = Files.writeString(tempDir.resolve("01-imported.md"), "新增文件内容");
        Path skippedFile = Files.writeString(tempDir.resolve("02-skipped.md"), "未变化文件内容");
        Path updatedFile = Files.writeString(tempDir.resolve("03-updated.md"), "更新后的文件内容");

        RagProperties ragProperties = new RagProperties();
        ragProperties.getKnowledgeBase().setDefaultDirectory(tempDir.toString());
        ragProperties.getKnowledgeBase().setDefaultCategory("知识");
        ragProperties.getKnowledgeBase().setRecursiveSync(true);

        KnowledgeDocumentService knowledgeDocumentService = mock(KnowledgeDocumentService.class);
        VectorStoreCleanupRepository vectorStoreCleanupRepository = mock(VectorStoreCleanupRepository.class);
        VectorStore vectorStore = mock(VectorStore.class);

        KnowledgeDocumentEntity skipped = new KnowledgeDocumentEntity();
        skipped.setId(2L);
        skipped.setFilePath(skippedFile.toAbsolutePath().toString());
        skipped.setFileHash(org.springframework.util.DigestUtils.md5DigestAsHex("未变化文件内容".getBytes(StandardCharsets.UTF_8)));

        KnowledgeDocumentEntity updated = new KnowledgeDocumentEntity();
        updated.setId(3L);
        updated.setFilePath(updatedFile.toAbsolutePath().toString());
        updated.setFileHash("old-hash");

        when(knowledgeDocumentService.getByFilePath(importedFile.toAbsolutePath().toString())).thenReturn(null);
        when(knowledgeDocumentService.getByFilePath(skippedFile.toAbsolutePath().toString())).thenReturn(skipped);
        when(knowledgeDocumentService.getByFilePath(updatedFile.toAbsolutePath().toString())).thenReturn(updated);

        KnowledgeDocumentEntity created = new KnowledgeDocumentEntity();
        created.setId(1L);
        when(knowledgeDocumentService.createPendingDocument(eq(importedFile.toAbsolutePath().toString()), anyString(), anyString()))
                .thenReturn(created);

        KnowledgeDirectorySyncServiceImpl service = new KnowledgeDirectorySyncServiceImpl(
                ragProperties,
                knowledgeDocumentService,
                new LocalKnowledgeLoader(),
                new KnowledgeChunker(ragProperties),
                vectorStoreCleanupRepository,
                vectorStore
        );

        SyncKnowledgeDirectoryRequest request = new SyncKnowledgeDirectoryRequest();
        request.setDirectoryPath(tempDir.toString());
        request.setCategory("知识");
        request.setRecursive(false);

        SyncKnowledgeDirectoryResponse response = service.syncDirectory(request);

        assertEquals(3, response.getScannedFiles());
        assertEquals(1, response.getImportedFiles());
        assertEquals(1, response.getUpdatedFiles());
        assertEquals(1, response.getSkippedFiles());
        assertEquals(0, response.getFailedFiles());

        verify(vectorStore, times(2)).add(any());
        verify(vectorStoreCleanupRepository).deleteBySourcePath(updatedFile.toAbsolutePath().toString());
        verify(knowledgeDocumentService).markBeforeResync(anyLong(), anyString(), anyString());
        verify(knowledgeDocumentService, times(2)).markImported(anyLong(), anyString());
        verify(knowledgeDocumentService, never()).markImportFailed(anyLong(), anyString());
    }
}
