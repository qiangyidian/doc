package com.qiange.ragdemo.service;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.dto.AskQuestionResponse;
import com.qiange.ragdemo.dto.ReferenceChunkResponse;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirdStageContractTest {

    @Test
    void thirdStageDtoAndModelContractsShouldExist() throws Exception {
        assertBeanProperty(AskQuestionResponse.class, "contextLengthBeforeCompression");
        assertBeanProperty(AskQuestionResponse.class, "contextLengthAfterCompression");

        assertBeanProperty(ReferenceChunkResponse.class, "rerankScore");
        assertBeanProperty(ReferenceChunkResponse.class, "compressedContent");
        assertBeanProperty(ReferenceChunkResponse.class, "finalSelectionStatus");

        assertBeanProperty(RetrievedChunk.class, "rerankScore");
        assertBeanProperty(RetrievedChunk.class, "compressedContent");
        assertBeanProperty(RetrievedChunk.class, "selectedForAnswer");
    }

    @Test
    void thirdStageConstantsAndTypesShouldBePresent() throws Exception {
        assertEquals("SELECTED", readStaticString(RagConstants.class, "PIPELINE_SELECTED"));
        assertEquals("DROPPED", readStaticString(RagConstants.class, "PIPELINE_DROPPED"));

        assertClassWithProperties(
                "com.qiange.ragdemo.dto.PipelineDebugRequest",
                Set.of("question", "category")
        );
        assertClassWithProperties(
                "com.qiange.ragdemo.dto.PipelineDebugResponse",
                Set.of(
                        "queries",
                        "retrievedChunks",
                        "rerankedChunks",
                        "compressedChunks",
                        "contextLengthBeforeCompression",
                        "contextLengthAfterCompression"
                )
        );
        assertClassWithProperties(
                "com.qiange.ragdemo.service.model.CompressedContextResult",
                Set.of("chunks", "contextLengthBeforeCompression", "contextLengthAfterCompression")
        );
        assertClassWithProperties(
                "com.qiange.ragdemo.service.model.RerankDecision",
                Set.of("index", "score", "keep", "reason")
        );
        assertClassWithProperties(
                "com.qiange.ragdemo.service.model.RerankResultEnvelope",
                Set.of("results")
        );
        assertClassWithProperties(
                "com.qiange.ragdemo.service.model.CompressionDecision",
                Set.of("index", "keep", "compressedContent")
        );
        assertClassWithProperties(
                "com.qiange.ragdemo.service.model.CompressionResultEnvelope",
                Set.of("results")
        );

        Class.forName("com.qiange.ragdemo.service.RerankService");
        Class.forName("com.qiange.ragdemo.service.ContextCompressionService");
        Class.forName("com.qiange.ragdemo.controller.PipelineDebugController");
    }

    private static void assertBeanProperty(Class<?> type, String propertyName) throws Exception {
        Set<String> propertyNames = Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toSet());
        assertTrue(propertyNames.contains(propertyName), () -> type.getSimpleName() + " should expose property " + propertyName);
    }

    private static void assertClassWithProperties(String className, Set<String> expectedProperties) throws Exception {
        Class<?> type = Class.forName(className);
        Set<String> propertyNames = Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toSet());
        assertTrue(propertyNames.containsAll(expectedProperties), () -> className + " should expose properties " + expectedProperties);
    }

    private static String readStaticString(Class<?> type, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }
}
