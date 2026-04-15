CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS kb_document (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL UNIQUE,
    file_hash VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(512),
    last_sync_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kb_document_status ON kb_document(status);
CREATE INDEX IF NOT EXISTS idx_kb_document_category ON kb_document(category);
CREATE INDEX IF NOT EXISTS idx_kb_document_file_hash ON kb_document(file_hash);

CREATE TABLE IF NOT EXISTS vector_store_ollama (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(768)
);

CREATE INDEX IF NOT EXISTS vector_store_ollama_embedding_hnsw_idx
    ON vector_store_ollama
    USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS vector_store_ollama_content_trgm_idx
    ON vector_store_ollama
    USING gin (content gin_trgm_ops);
