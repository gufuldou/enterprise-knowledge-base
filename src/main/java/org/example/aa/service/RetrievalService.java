package org.example.aa.service;

import org.example.aa.entity.DocumentChunk;
import org.example.aa.mapper.DocumentChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final DocumentChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final JsonMapper jsonMapper;

    public RetrievalService(DocumentChunkMapper chunkMapper,
                            EmbeddingService embeddingService,
                            JsonMapper jsonMapper) {
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
        this.jsonMapper = jsonMapper;
    }

    public record RetrievedChunk(Long documentId, Integer chunkIndex, String content, double score) {
    }

    public List<RetrievedChunk> retrieve(String query, int topK) {
        float[] queryVector = embeddingService.embed(query);
        String queryVectorStr = jsonMapper.writeValueAsString(queryVector);
        List<DocumentChunk> hits = chunkMapper.search(queryVectorStr, topK);

        List<RetrievedChunk> result = hits.stream()
                .map(h -> new RetrievedChunk(
                        h.getDocumentId(), h.getChunkIndex(), h.getContent(), h.getSimilarity()))
                .toList();
        if (!result.isEmpty()) {
            log.info("检索完成: 命中 {} 条, 最高分 {}", result.size(), result.get(0).score());
        }
        return result;
    }
}
