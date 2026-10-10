package org.example.aa.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.aa.entity.DocumentChunk;

import java.util.List;

@Mapper
public interface DocumentChunkMapper {

    @Insert("""
        INSERT INTO document_chunk (document_id, chunk_index, content, embedding)
        VALUES (#{documentId}, #{chunkIndex}, #{content}, #{embedding}::vector)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DocumentChunk chunk);

    @Select("""
        SELECT
            document_id AS documentId,
            chunk_index AS chunkIndex,
            content,
            1 - (embedding <=> #{queryVector}::vector) AS similarity
        FROM document_chunk
        WHERE embedding IS NOT NULL
        ORDER BY embedding <=> #{queryVector}::vector
        LIMIT #{topK}
        """)
    List<DocumentChunk> search(@Param("queryVector") String queryVector, @Param("topK") int topK);

    @Select("""
        SELECT
            id,
            document_id AS documentId,
            chunk_index AS chunkIndex,
            content,
            embedding,
            created_at AS createdAt
        FROM document_chunk
        WHERE document_id = #{documentId}
        ORDER BY chunk_index
        """)
    List<DocumentChunk> findByDocumentId(Long documentId);

    @Delete("DELETE FROM document_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(Long documentId);
}
