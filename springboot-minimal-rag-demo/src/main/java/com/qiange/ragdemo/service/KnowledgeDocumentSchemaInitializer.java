package com.qiange.ragdemo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 兼容旧版 kb_document 表结构，确保第二阶段台账字段可用。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeDocumentSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS kb_document (
                    id BIGSERIAL PRIMARY KEY,
                    file_name VARCHAR(255) NOT NULL,
                    file_path VARCHAR(512),
                    file_hash VARCHAR(64),
                    category VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    remark VARCHAR(512),
                    last_sync_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS file_path VARCHAR(512)");
        jdbcTemplate.execute("ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS file_hash VARCHAR(64)");
        jdbcTemplate.execute("ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS remark VARCHAR(512)");
        jdbcTemplate.execute("ALTER TABLE kb_document ADD COLUMN IF NOT EXISTS last_sync_at TIMESTAMP");

        jdbcTemplate.execute("""
                UPDATE kb_document
                SET file_path = COALESCE(file_path, source_path)
                WHERE file_path IS NULL
                """);
        jdbcTemplate.execute("""
                UPDATE kb_document
                SET file_hash = COALESCE(file_hash, doc_id)
                WHERE file_hash IS NULL
                """);
        jdbcTemplate.execute("""
                UPDATE kb_document
                SET remark = COALESCE(remark, '历史数据迁移补齐')
                WHERE remark IS NULL
                """);
        jdbcTemplate.execute("""
                UPDATE kb_document
                SET last_sync_at = COALESCE(last_sync_at, updated_at, created_at, CURRENT_TIMESTAMP)
                WHERE last_sync_at IS NULL
                """);
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_name = 'kb_document' AND column_name = 'doc_id'
                    ) THEN
                        EXECUTE 'UPDATE kb_document SET doc_id = COALESCE(doc_id, file_hash) WHERE doc_id IS NULL';
                        EXECUTE 'ALTER TABLE kb_document ALTER COLUMN doc_id DROP NOT NULL';
                    END IF;

                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_name = 'kb_document' AND column_name = 'title'
                    ) THEN
                        EXECUTE 'UPDATE kb_document SET title = COALESCE(title, file_name) WHERE title IS NULL';
                        EXECUTE 'ALTER TABLE kb_document ALTER COLUMN title DROP NOT NULL';
                    END IF;

                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_name = 'kb_document' AND column_name = 'source_path'
                    ) THEN
                        EXECUTE 'UPDATE kb_document SET source_path = COALESCE(source_path, file_path) WHERE source_path IS NULL';
                        EXECUTE 'ALTER TABLE kb_document ALTER COLUMN source_path DROP NOT NULL';
                    END IF;

                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_name = 'kb_document' AND column_name = 'chunk_count'
                    ) THEN
                        EXECUTE 'UPDATE kb_document SET chunk_count = COALESCE(chunk_count, 0) WHERE chunk_count IS NULL';
                        EXECUTE 'ALTER TABLE kb_document ALTER COLUMN chunk_count SET DEFAULT 0';
                        EXECUTE 'ALTER TABLE kb_document ALTER COLUMN chunk_count DROP NOT NULL';
                    END IF;
                END $$;
                """);

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_kb_document_status ON kb_document(status)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_kb_document_category ON kb_document(category)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_kb_document_file_hash ON kb_document(file_hash)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_kb_document_file_path ON kb_document(file_path)");
    }
}
