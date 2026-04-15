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
                () -> assertTrue(fieldNames.contains("fileName")),
                () -> assertTrue(fieldNames.contains("filePath")),
                () -> assertTrue(fieldNames.contains("fileHash")),
                () -> assertTrue(fieldNames.contains("category")),
                () -> assertTrue(fieldNames.contains("status")),
                () -> assertTrue(fieldNames.contains("remark")),
                () -> assertTrue(fieldNames.contains("lastSyncAt")),
                () -> assertFalse(fieldNames.contains("docId")),
                () -> assertFalse(fieldNames.contains("title")),
                () -> assertFalse(fieldNames.contains("sourcePath")),
                () -> assertFalse(fieldNames.contains("chunkCount"))
        );
    }

    @Test
    void mapperShouldUseCurrentKbDocumentColumns() throws IOException {
        String mapperXml = Files.readString(Path.of("src/main/resources/mapper/KnowledgeDocumentMapper.xml"));

        assertAll(
                () -> assertTrue(mapperXml.contains("file_name")),
                () -> assertTrue(mapperXml.contains("file_path")),
                () -> assertTrue(mapperXml.contains("file_hash")),
                () -> assertTrue(mapperXml.contains("remark")),
                () -> assertTrue(mapperXml.contains("last_sync_at")),
                () -> assertFalse(mapperXml.contains("doc_id")),
                () -> assertFalse(mapperXml.contains("title")),
                () -> assertFalse(mapperXml.contains("source_path")),
                () -> assertFalse(mapperXml.contains("chunk_count"))
        );
    }

    @Test
    void createPendingDocumentShouldPrepareSchemaCompatibleEntity() throws Exception {
        CapturingKnowledgeDocumentMapper mapper = new CapturingKnowledgeDocumentMapper();
        KnowledgeDocumentServiceImpl service = new KnowledgeDocumentServiceImpl(mapper);

        KnowledgeDocumentEntity entity = service.createPendingDocument(
                "knowledge-base/01-rag-intro.md",
                "hash-001",
                "知识"
        );

        assertNotNull(entity);
        assertNotNull(mapper.insertedEntity);

        assertAll(
                () -> assertEquals("01-rag-intro.md", readField(mapper.insertedEntity, "fileName")),
                () -> assertEquals(Path.of("knowledge-base/01-rag-intro.md").toAbsolutePath().toString(),
                        readField(mapper.insertedEntity, "filePath")),
                () -> assertEquals("hash-001", readField(mapper.insertedEntity, "fileHash")),
                () -> assertEquals("知识", readField(mapper.insertedEntity, "category")),
                () -> assertEquals("PENDING", readField(mapper.insertedEntity, "status")),
                () -> assertEquals("首次发现文件，等待同步", readField(mapper.insertedEntity, "remark"))
        );
    }

    @Test
    void statusTransitionsShouldDelegateToMapperWithExpectedStates() {
        CapturingKnowledgeDocumentMapper mapper = new CapturingKnowledgeDocumentMapper();
        KnowledgeDocumentServiceImpl service = new KnowledgeDocumentServiceImpl(mapper);

        service.markBeforeResync(7L, "hash-002", "准备重同步");
        service.markImported(7L, "同步成功");
        service.markImportFailed(7L, "同步失败");

        assertAll(
                () -> assertEquals(7L, mapper.lastUpdatedId),
                () -> assertEquals("FAILED", mapper.lastUpdatedStatus),
                () -> assertEquals("同步失败", mapper.lastUpdatedRemark),
                () -> assertEquals("hash-002", mapper.lastUpdatedFileHashBeforeResync)
        );
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static final class CapturingKnowledgeDocumentMapper implements KnowledgeDocumentMapper {

        private KnowledgeDocumentEntity insertedEntity;
        private Long lastUpdatedId;
        private String lastUpdatedFileHashBeforeResync;
        private String lastUpdatedStatus;
        private String lastUpdatedRemark;

        @Override
        public KnowledgeDocumentEntity selectByFileHash(String fileHash) {
            return null;
        }

        @Override
        public KnowledgeDocumentEntity selectByFilePath(String filePath) {
            return null;
        }

        @Override
        public int insert(KnowledgeDocumentEntity entity) {
            this.insertedEntity = entity;
            return 1;
        }

        @Override
        public int updateBeforeResync(Long id, String fileHash, String status, String remark) {
            this.lastUpdatedId = id;
            this.lastUpdatedFileHashBeforeResync = fileHash;
            this.lastUpdatedStatus = status;
            this.lastUpdatedRemark = remark;
            return 1;
        }

        @Override
        public int updateAfterSync(Long id, String status, String remark) {
            this.lastUpdatedId = id;
            this.lastUpdatedStatus = status;
            this.lastUpdatedRemark = remark;
            return 1;
        }

        @Override
        public int updateFailed(Long id, String status, String remark) {
            this.lastUpdatedId = id;
            this.lastUpdatedStatus = status;
            this.lastUpdatedRemark = remark;
            return 1;
        }
    }
}
