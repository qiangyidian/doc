package com.qiange.ragdemo.service;

import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.loader.KnowledgeChunker;
import com.qiange.ragdemo.loader.LocalKnowledgeLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BundledKnowledgeBootstrapperTest {

    @TempDir
    Path tempDir;

    @Test
    void bootstrapIfNecessaryShouldLoadBundledFilesWhenVectorStoreIsEmpty() throws IOException {
        Path bootstrapFile = Files.writeString(tempDir.resolve("01-rag-intro.md"), "RAG intro");

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        LocalKnowledgeLoader localKnowledgeLoader = mock(LocalKnowledgeLoader.class);
        KnowledgeChunker knowledgeChunker = mock(KnowledgeChunker.class);
        VectorStore vectorStore = mock(VectorStore.class);

        RagProperties ragProperties = new RagProperties();
        ragProperties.getKnowledgeBase().setBootstrapPath(tempDir.toString());
        ragProperties.getKnowledgeBase().setBootstrapCategory("知识");

        Document originalDocument = new Document("RAG intro");
        Document chunkDocument = new Document("RAG intro chunk");

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        when(localKnowledgeLoader.load(bootstrapFile.toString(), "知识", null)).thenReturn(originalDocument);
        when(knowledgeChunker.split(originalDocument)).thenReturn(List.of(chunkDocument));

        BundledKnowledgeBootstrapper bootstrapper = new BundledKnowledgeBootstrapper(
                jdbcTemplate,
                localKnowledgeLoader,
                knowledgeChunker,
                vectorStore,
                ragProperties,
                "vector_store_ollama",
                768
        );

        assertDoesNotThrow(bootstrapper::bootstrapIfNecessary);

        verify(localKnowledgeLoader).load(bootstrapFile.toString(), "知识", null);
        verify(vectorStore).add(List.of(chunkDocument));
    }

    @Test
    void bootstrapIfNecessaryShouldSkipWhenVectorStoreAlreadyHasData() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        LocalKnowledgeLoader localKnowledgeLoader = mock(LocalKnowledgeLoader.class);
        KnowledgeChunker knowledgeChunker = mock(KnowledgeChunker.class);
        VectorStore vectorStore = mock(VectorStore.class);

        RagProperties ragProperties = new RagProperties();
        ragProperties.getKnowledgeBase().setBootstrapPath(tempDir.toString());

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(2);

        BundledKnowledgeBootstrapper bootstrapper = new BundledKnowledgeBootstrapper(
                jdbcTemplate,
                localKnowledgeLoader,
                knowledgeChunker,
                vectorStore,
                ragProperties,
                "vector_store_ollama",
                768
        );

        assertDoesNotThrow(bootstrapper::bootstrapIfNecessary);

        verify(localKnowledgeLoader, never()).load(anyString(), anyString(), anyString());
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.<List<Document>>any());
    }
}
