-- 先开启 pgvector 扩展。
-- 如果这一步没做，后面建向量列时会直接报错：type "vector" does not exist。
CREATE EXTENSION IF NOT EXISTS vector;

-- 这个扩展用于生成 UUID。
-- Spring AI 的默认向量表通常会用到 UUID 主键，这里直接提前准备好。
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 文档台账表。
-- 这张表不负责保存向量，只负责保存：
-- 文档编号、标题、分类、原始文件路径、导入状态、块数量等管理信息。
CREATE TABLE IF NOT EXISTS kb_document (
                                           id BIGSERIAL PRIMARY KEY,
                                           doc_id VARCHAR(64) NOT NULL UNIQUE,
                                           title VARCHAR(255) NOT NULL,
                                           file_name VARCHAR(255) NOT NULL,
                                           category VARCHAR(64) NOT NULL,
                                           source_path VARCHAR(512) NOT NULL,
                                           status VARCHAR(32) NOT NULL,
                                           chunk_count INT NOT NULL DEFAULT 0,
                                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 按状态和分类建索引，方便后面做管理查询。
CREATE INDEX IF NOT EXISTS idx_kb_document_status ON kb_document(status);
CREATE INDEX IF NOT EXISTS idx_kb_document_category ON kb_document(category);

-- 向量存储表。
-- 这个表是最基础的 PGVector 表结构：
-- content 存文本块正文
-- metadata 存元数据 JSON
-- embedding 存向量数组
# CREATE TABLE IF NOT EXISTS vector_store (
#                                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
#                                             content TEXT NOT NULL,
#                                             metadata JSON,
#                                             embedding VECTOR(1536)
# );
-- 向量存储表。
-- 注意：这里将 VECTOR 维度从 1536 改为了 1024，以适配 bge-m3 模型
CREATE TABLE IF NOT EXISTS vector_store (
                                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                            content TEXT NOT NULL,
                                            metadata JSON,
                                            embedding VECTOR(1024)
);

-- 向量索引。
-- 这里使用 HNSW，因为它是教程里推荐的 PGVector 入门首选。
CREATE INDEX IF NOT EXISTS vector_store_embedding_hnsw_idx
    ON vector_store
    USING hnsw (embedding vector_cosine_ops);