package com.qiange.ragdemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.dto.RetrievalEvaluationResponse;
import com.qiange.ragdemo.service.impl.RetrievalEvaluationServiceImpl;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetrievalEvaluationServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void evaluateShouldCalculateHitRateAndMrr() throws IOException {
        Path caseFile = tempDir.resolve("eval.json");
        Files.writeString(caseFile, """
                [
                  {
                    "question": "问题1",
                    "category": "知识",
                    "expectedSourceFileName": "03-hybrid-retrieval.md",
                    "description": "case1"
                  },
                  {
                    "question": "问题2",
                    "category": "知识",
                    "expectedSourceFileName": "04-query-rewrite.md",
                    "description": "case2"
                  }
                ]
                """);

        RagProperties ragProperties = new RagProperties();
        ragProperties.getEvaluation().setCaseFile(caseFile.toString());

        QueryRewriteService queryRewriteService = mock(QueryRewriteService.class);
        HybridRetrievalService hybridRetrievalService = mock(HybridRetrievalService.class);

        when(queryRewriteService.rewriteQueries("问题1")).thenReturn(List.of("问题1", "改写1"));
        when(queryRewriteService.rewriteQueries("问题2")).thenReturn(List.of("问题2"));
        when(hybridRetrievalService.retrieve(List.of("问题1", "改写1"), "知识"))
                .thenReturn(List.of(chunk("03-hybrid-retrieval.md"), chunk("其他.md")));
        when(hybridRetrievalService.retrieve(List.of("问题2"), "知识"))
                .thenReturn(List.of(chunk("其他.md"), chunk("04-query-rewrite.md")));

        RetrievalEvaluationServiceImpl service = new RetrievalEvaluationServiceImpl(
                ragProperties,
                queryRewriteService,
                hybridRetrievalService,
                new ObjectMapper()
        );

        RetrievalEvaluationResponse response = service.evaluate();

        assertEquals(2, response.getTotalCases());
        assertEquals(1.0, response.getHitRate());
        assertEquals(0.75, response.getMrr());
        assertEquals(1, response.getCaseResults().get(0).getRank());
        assertEquals(2, response.getCaseResults().get(1).getRank());
    }

    private static RetrievedChunk chunk(String sourceFileName) {
        return RetrievedChunk.builder()
                .uniqueKey(sourceFileName + "#0")
                .document(new Document("content", Map.of(
                        RagConstants.METADATA_SOURCE_FILE_NAME, sourceFileName,
                        RagConstants.METADATA_SOURCE_PATH, "/tmp/" + sourceFileName,
                        RagConstants.METADATA_CHUNK_INDEX, 0
                )))
                .retrievalSource(RagConstants.RETRIEVAL_SOURCE_VECTOR)
                .fusionScore(1.0)
                .build();
    }
}
