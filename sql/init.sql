-- =============================================================
-- 企业知识库问答系统 - 初始化表结构（PostgreSQL + pgvector）
-- 执行前提：
--   1. 已安装 PostgreSQL 并创建 knowledge_base 数据库
--   2. pgvector 扩展由本文件第一句自动创建
-- 执行方式：psql -U postgres -d knowledge_base -f init.sql
-- =============================================================

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 1. 文档元数据表
CREATE TABLE IF NOT EXISTS kb_document (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    file_type   VARCHAR(20)  NOT NULL,
    file_size   BIGINT       NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED',
    chunk_count INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. 文档分块表（向量用 pgvector 的 vector 类型存储）
CREATE TABLE IF NOT EXISTS document_chunk (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT      NOT NULL,
    chunk_index INT         NOT NULL,
    content     TEXT        NOT NULL,
    embedding   vector(1024),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id ON document_chunk(document_id);
-- HNSW 近似最近邻索引：余弦距离（vector_cosine_ops）
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding ON document_chunk USING hnsw (embedding vector_cosine_ops);
-- 关键词通道：pg_trgm 三元组索引，用于混合检索的词法匹配
CREATE INDEX IF NOT EXISTS idx_document_chunk_content_trgm ON document_chunk USING GIN (content gin_trgm_ops);
