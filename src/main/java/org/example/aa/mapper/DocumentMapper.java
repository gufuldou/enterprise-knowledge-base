package org.example.aa.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.example.aa.entity.KnowledgeDocument;

import java.util.List;

@Mapper
public interface DocumentMapper {

    @Insert("""
        INSERT INTO kb_document
            (title, file_name, file_path, file_type, file_size, status, chunk_count)
        VALUES
            (#{title}, #{fileName}, #{filePath}, #{fileType}, #{fileSize}, #{status}, #{chunkCount})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocument document);

    @Select("""
        SELECT
            id,
            title,
            file_name AS fileName,
            file_path AS filePath,
            file_type AS fileType,
            file_size AS fileSize,
            status,
            chunk_count AS chunkCount,
            created_at AS createdAt
        FROM kb_document
        ORDER BY created_at DESC
        """)
    List<KnowledgeDocument> findAll();

    @Select("""
        SELECT
            id,
            title,
            file_name AS fileName,
            file_path AS filePath,
            file_type AS fileType,
            file_size AS fileSize,
            status,
            chunk_count AS chunkCount,
            created_at AS createdAt
        FROM kb_document
        WHERE id = #{id}
        """)
    KnowledgeDocument findById(Long id);

    @Delete("DELETE FROM kb_document WHERE id = #{id}")
    int deleteById(Long id);
}
