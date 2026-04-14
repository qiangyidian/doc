package com.qiange.ragdemo.service;

import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import com.qiange.ragdemo.mapper.KnowledgeDocumentMapper;
import com.qiange.ragdemo.service.impl.KnowledgeDocumentServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeDocumentContractTest {

    @Test
    void entityShouldMatchKbDocumentSchemaContract() {
        List<String> fieldNames = Arrays.stream(KnowledgeDocumentEntity.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertAll(
                () -> assertTrue(fieldNames.contains("docId")),
                () -> assertTrue(fieldNames.contains("title")),
                () -> assertTrue(fieldNames.contains("fileName")),
                () -> assertTrue(fieldNames.contains("category")),
                () -> assertTrue(fieldNames.contains("sourcePath")),
                () -> assertTrue(fieldNames.contains("status")),
                () -> assertTrue(fieldNames.contains("chunkCount")),
                () -> assertFalse(fieldNames.contains("filePath")),
                () -> assertFalse(fieldNames.contains("fileHash")),
                () -> assertFalse(fieldNames.contains("remark"))
        );
    }

    @Test
    void mapperShouldUseCurrentKbDocumentColumns() throws IOException {
        String mapperXml = Files.readString(Path.of("src/main/resources/mapper/KnowledgeDocumentMapper.xml"));

        assertAll(
                () -> assertTrue(mapperXml.contains("doc_id")),
                () -> assertTrue(mapperXml.contains("title")),
                () -> assertTrue(mapperXml.contains("source_path")),
                () -> assertTrue(mapperXml.contains("chunk_count")),
                () -> assertFalse(mapperXml.contains("file_path")),
                () -> assertFalse(mapperXml.contains("file_hash")),
                () -> assertFalse(mapperXml.contains("remark"))
        );
    }

    @Test
    void createPendingDocumentShouldPrepareSchemaCompatibleEntity() throws Exception {
        CapturingKnowledgeDocumentMapper mapper = new CapturingKnowledgeDocumentMapper();
        KnowledgeDocumentServiceImpl service = new KnowledgeDocumentServiceImpl(mapper);

        KnowledgeDocumentEntity entity = service.createPendingDocument(
                "knowledge-base/01-rag-intro.md",
                "hash-001",
                "tutorial",
                null
        );

        assertNotNull(entity);
        assertNotNull(mapper.insertedEntity);

        assertAll(
                () -> assertEquals("hash-001", readField(mapper.insertedEntity, "docId")),
                () -> assertEquals("01-rag-intro.md", readField(mapper.insertedEntity, "title")),
                () -> assertEquals("01-rag-intro.md", readField(mapper.insertedEntity, "fileName")),
                () -> assertEquals(Path.of("knowledge-base/01-rag-intro.md").toAbsolutePath().toString(),
                        readField(mapper.insertedEntity, "sourcePath")),
                () -> assertEquals("tutorial", readField(mapper.insertedEntity, "category")),
                () -> assertEquals("PENDING", readField(mapper.insertedEntity, "status")),
                () -> assertEquals(0, readField(mapper.insertedEntity, "chunkCount"))
        );
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static final class CapturingKnowledgeDocumentMapper implements KnowledgeDocumentMapper {

        private KnowledgeDocumentEntity insertedEntity;

        @Override
        public KnowledgeDocumentEntity selectByDocumentId(String documentId) {
            return null;
        }

        @Override
        public int insert(KnowledgeDocumentEntity entity) {
            this.insertedEntity = entity;
            return 1;
        }

        @Override
        public int updateStatusAndChunkCount(Long id, String status, Integer chunkCount) {
            return 1;
        }
    }
}
