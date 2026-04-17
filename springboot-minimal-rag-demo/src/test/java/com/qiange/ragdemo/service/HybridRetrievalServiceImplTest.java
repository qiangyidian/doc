package com.qiange.ragdemo.service;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.repository.KeywordSearchRepository;
import com.qiange.ragdemo.service.impl.HybridRetrievalServiceImpl;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridRetrievalServiceImplTest {

    @Test
    void retrieveShouldFuseVectorAndKeywordResultsIntoThirdStageCandidates() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        KeywordSearchRepository keywordSearchRepository = mock(KeywordSearchRepository.class);

        RagProperties ragProperties = new RagProperties();
        ragProperties.getRetrieval().setVectorTopK(3);
        ragProperties.getRetrieval().setKeywordTopK(3);
        ragProperties.getRetrieval().setRrfK(60);
        setField(ragProperties.getRetrieval(), "candidateTopK", 2);

        Document chunkA = document("03-hybrid-retrieval.md", "/docs/03-hybrid-retrieval.md", 0, "混合检索");
        Document chunkB = document("04-query-rewrite.md", "/docs/04-query-rewrite.md", 0, "查询改写");
        Document chunkC = document("01-rag-intro.md", "/docs/01-rag-intro.md", 0, "RAG 简介");

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(chunkA, chunkB))
                .thenReturn(List.of(chunkA));

        when(keywordSearchRepository.search("为什么接口名检索不稳定", "知识", 3, 0.08))
                .thenReturn(List.of(chunkA, chunkC));
        when(keywordSearchRepository.search("接口名 字段名 关键词 检索", "知识", 3, 0.08))
                .thenReturn(List.of(chunkA));

        HybridRetrievalServiceImpl service = new HybridRetrievalServiceImpl(
                vectorStore,
                keywordSearchRepository,
                ragProperties
        );

        List<RetrievedChunk> chunks = service.retrieve(
                List.of("为什么接口名检索不稳定", "接口名 字段名 关键词 检索"),
                "知识"
        );

        assertEquals(2, chunks.size());
        assertEquals("03-hybrid-retrieval.md", chunks.get(0).getDocument().getMetadata().get(RagConstants.METADATA_SOURCE_FILE_NAME));
        assertEquals(RagConstants.RETRIEVAL_SOURCE_HYBRID, chunks.get(0).getRetrievalSource());
        assertTrue(chunks.get(0).getFusionScore() > chunks.get(1).getFusionScore());
        assertEquals(null, readField(chunks.get(0), "rerankScore"));
        assertEquals(null, readField(chunks.get(0), "compressedContent"));
        assertFalse((Boolean) readField(chunks.get(0), "selectedForAnswer"));
    }

    private static Document document(String sourceFileName, String sourcePath, int chunkIndex, String content) {
        return new Document(content, Map.of(
                RagConstants.METADATA_SOURCE_FILE_NAME, sourceFileName,
                RagConstants.METADATA_SOURCE_PATH, sourcePath,
                RagConstants.METADATA_CHUNK_INDEX, chunkIndex
        ));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
