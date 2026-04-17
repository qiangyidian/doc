package com.qiange.ragdemo.service;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.service.model.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RerankServiceImplTest {

    @Test
    void rerankShouldFallbackToFusionOrderWhenDisabled() throws Exception {
        RagProperties ragProperties = new RagProperties();
        Object rerankProperties = readNestedProperty(ragProperties, "rerank");
        setField(rerankProperties, "enabled", false);
        setField(rerankProperties, "topK", 2);
        setField(rerankProperties, "minScore", 55);
        setField(rerankProperties, "maxContentLengthPerChunk", 350);

        Object service = newServiceInstance(
                "com.qiange.ragdemo.service.impl.RerankServiceImpl",
                mock(ChatClient.class),
                ragProperties
        );

        @SuppressWarnings("unchecked")
        List<RetrievedChunk> rerankedChunks = (List<RetrievedChunk>) invoke(
                service,
                "rerank",
                new Class<?>[]{String.class, List.class},
                "为什么要做精排",
                new ArrayList<>(List.of(
                        chunk("A", "alpha", 0.92),
                        chunk("B", "beta", 0.45),
                        chunk("C", "gamma", 0.76)
                ))
        );

        assertEquals(2, rerankedChunks.size());
        assertEquals("A", rerankedChunks.get(0).getDocument().getMetadata().get(RagConstants.METADATA_SOURCE_FILE_NAME));
        assertEquals("C", rerankedChunks.get(1).getDocument().getMetadata().get(RagConstants.METADATA_SOURCE_FILE_NAME));
        assertEquals(0.92, ((Number) readField(rerankedChunks.get(0), "rerankScore")).doubleValue(), 0.0001);
        assertEquals(0.76, ((Number) readField(rerankedChunks.get(1), "rerankScore")).doubleValue(), 0.0001);
        assertTrue((Boolean) readField(rerankedChunks.get(0), "selectedForAnswer"));
        assertTrue((Boolean) readField(rerankedChunks.get(1), "selectedForAnswer"));
    }

    private static RetrievedChunk chunk(String name, String content, double fusionScore) {
        return RetrievedChunk.builder()
                .uniqueKey(name)
                .document(new Document(content, Map.of(
                        RagConstants.METADATA_SOURCE_FILE_NAME, name,
                        RagConstants.METADATA_SOURCE_PATH, "/docs/" + name + ".md",
                        RagConstants.METADATA_CHUNK_INDEX, 0
                )))
                .retrievalSource(RagConstants.RETRIEVAL_SOURCE_HYBRID)
                .fusionScore(fusionScore)
                .build();
    }

    private static Object newServiceInstance(String className, Object... dependencies) throws Exception {
        Class<?> type = Class.forName(className);
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterCount() != dependencies.length) {
                continue;
            }
            Object[] args = arrangeArguments(constructor.getParameterTypes(), dependencies);
            if (args != null) {
                return constructor.newInstance(args);
            }
        }
        throw new IllegalStateException("No matching constructor found for " + className);
    }

    private static Object[] arrangeArguments(Class<?>[] parameterTypes, Object[] dependencies) {
        Object[] args = new Object[parameterTypes.length];
        boolean[] used = new boolean[dependencies.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            boolean matched = false;
            for (int j = 0; j < dependencies.length; j++) {
                if (!used[j] && parameterTypes[i].isInstance(dependencies[j])) {
                    args[i] = dependencies[j];
                    used[j] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return null;
            }
        }
        return args;
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }

    private static Object readNestedProperty(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
