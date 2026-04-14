package com.qiange.ragdemo.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaCompatibilityConfigTest {

    @Test
    void applicationYamlShouldUseLocalOllamaModelsAndVectorStoreContract() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(applicationYaml.contains("ollama:"));
        assertTrue(applicationYaml.contains("base-url: http://localhost:11434"));
        assertTrue(applicationYaml.contains("model: deepseek-r1:1.5b"));
        assertTrue(applicationYaml.contains("model: nomic-embed-text:v1.5"));
        assertTrue(applicationYaml.contains("dimensions: 768"));
        assertTrue(applicationYaml.contains("table-name: vector_store_ollama"));
        assertFalse(applicationYaml.contains("openai:"));
        assertFalse(applicationYaml.contains("api.siliconflow.com"));
    }

    @Test
    void pomShouldUseOllamaStarterInsteadOfOpenAiStarter() throws IOException {
        String pomXml = Files.readString(Path.of("pom.xml"));

        assertTrue(pomXml.contains("spring-ai-starter-model-ollama"));
        assertFalse(pomXml.contains("spring-ai-starter-model-openai"));
    }
}
