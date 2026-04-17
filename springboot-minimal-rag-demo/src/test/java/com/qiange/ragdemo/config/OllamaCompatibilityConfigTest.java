package com.qiange.ragdemo.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaCompatibilityConfigTest {

    @Test
    void applicationYamlShouldUseLocalOllamaModelsAndEnhancedRetrievalContract() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(applicationYaml.contains("ollama:"));
        assertTrue(applicationYaml.contains("base-url: http://localhost:11434"));
        assertTrue(applicationYaml.contains("model: deepseek-r1:1.5b"));
        assertTrue(applicationYaml.contains("model: nomic-embed-text:v1.5"));
        assertTrue(applicationYaml.contains("dimensions: 768"));
        assertTrue(applicationYaml.contains("table-name: vector_store_ollama"));
        assertTrue(applicationYaml.contains("default-category: 知识"));
        assertTrue(applicationYaml.contains("default-directory: knowledge-base"));
        assertTrue(applicationYaml.contains("recursive-sync: true"));
        assertTrue(applicationYaml.contains("vector-top-k: 6"));
        assertTrue(applicationYaml.contains("keyword-top-k: 6"));
        assertTrue(applicationYaml.contains("trgm-similarity-threshold: 0.08"));
        assertTrue(applicationYaml.contains("candidate-top-k: 8"));
        assertTrue(applicationYaml.contains("rrf-k: 60"));
        assertTrue(applicationYaml.contains("query-rewrite-enabled: true"));
        assertTrue(applicationYaml.contains("rewrite-query-count: 2"));
        assertTrue(applicationYaml.contains("rerank:"));
        assertTrue(applicationYaml.contains("enabled: true"));
        assertTrue(applicationYaml.contains("top-k: 4"));
        assertTrue(applicationYaml.contains("min-score: 55"));
        assertTrue(applicationYaml.contains("max-content-length-per-chunk: 350"));
        assertTrue(applicationYaml.contains("compression:"));
        assertTrue(applicationYaml.contains("deduplicate-before-llm: true"));
        assertTrue(applicationYaml.contains("max-chars-per-chunk: 260"));
        assertTrue(applicationYaml.contains("max-total-context-chars: 1800"));
        assertTrue(applicationYaml.contains("drop-empty-chunks: true"));
        assertTrue(applicationYaml.contains("case-file: retrieval-eval/tutorial-eval-cases.json"));
        assertFalse(applicationYaml.contains("fusion-top-k:"));
    }

    @Test
    void pomShouldUseOllamaStarterInsteadOfOpenAiStarter() throws IOException {
        String pomXml = Files.readString(Path.of("pom.xml"));

        assertTrue(pomXml.contains("spring-ai-starter-model-ollama"));
        assertFalse(pomXml.contains("spring-ai-starter-model-openai"));
    }

    @Test
    void initSqlShouldPrepareTrigramSupportForOllamaVectorTable() throws IOException {
        String initSql = Files.readString(Path.of("init/postgres/init.sql"));

        assertTrue(initSql.contains("CREATE EXTENSION IF NOT EXISTS pg_trgm"));
        assertTrue(initSql.contains("CREATE TABLE IF NOT EXISTS vector_store_ollama"));
        assertTrue(initSql.contains("embedding VECTOR(768)"));
        assertTrue(initSql.contains("gin_trgm_ops"));
        assertTrue(initSql.contains("metadata JSONB"));
    }
}
