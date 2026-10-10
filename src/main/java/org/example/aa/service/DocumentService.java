package org.example.aa.service;

import tools.jackson.databind.json.JsonMapper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.example.aa.entity.DocumentChunk;
import org.example.aa.entity.KnowledgeDocument;
import org.example.aa.mapper.DocumentChunkMapper;
import org.example.aa.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "txt", "docx");
    private static final Tika TIKA = new Tika();

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final JsonMapper jsonMapper;

    public DocumentService(DocumentMapper documentMapper,
                           DocumentChunkMapper chunkMapper,
                           EmbeddingService embeddingService,
                           JsonMapper jsonMapper) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public KnowledgeDocument upload(MultipartFile file) throws IOException {
        String originalName = validateAndGetName(file);
        String extension = extensionOf(originalName);
        byte[] bytes = file.getBytes();

        String text = extractText(bytes);
        String storedPath = storeFile(bytes, extension);
        List<String> chunks = chunk(text);

        List<float[]> vectors = chunks.isEmpty()
                ? List.of()
                : embeddingService.embedBatch(chunks);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(stripExtension(originalName));
        document.setFileName(originalName);
        document.setFilePath(storedPath);
        document.setFileType(extension);
        document.setFileSize((long) bytes.length);
        document.setStatus(chunks.isEmpty() ? "UPLOADED" : "VECTORIZED");
        document.setChunkCount(chunks.size());
        documentMapper.insert(document);

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setEmbedding(jsonMapper.writeValueAsString(vectors.get(i)));
            chunkMapper.insert(chunk);
        }
        log.info("文档上传成功: id={}, title={}, chunks={}", document.getId(), document.getTitle(), chunks.size());
        return document;
    }

    @Transactional
    public void delete(Long documentId) {
        KnowledgeDocument document = documentMapper.findById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在: id=" + documentId);
        }
        chunkMapper.deleteByDocumentId(documentId);
        documentMapper.deleteById(documentId);
        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            log.warn("删除本地文件失败: {}", document.getFilePath(), e);
        }
        log.info("文档已删除: id={}, title={}", documentId, document.getTitle());
    }

    private String extractText(byte[] bytes) throws IOException {
        try {
            return TIKA.parseToString(new ByteArrayInputStream(bytes));
        } catch (TikaException e) {
            throw new IOException("文档文本提取失败", e);
        }
    }

    private String validateAndGetName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String safeName = Paths.get(originalName).getFileName().toString();
        String extension = extensionOf(safeName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 PDF、TXT 和 DOCX 文件");
        }
        return safeName;
    }

    private String storeFile(byte[] bytes, String extension) throws IOException {
        Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        Files.write(target, bytes);
        return target.toString();
    }

    private List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        int length = cleaned.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);
            chunks.add(cleaned.substring(start, end));
            if (end == length) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf(".");
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf(".");
        if (dot <= 0 || dot == name.length() - 1) {
            return null;
        }
        return name.substring(dot + 1).toLowerCase();
    }
}
