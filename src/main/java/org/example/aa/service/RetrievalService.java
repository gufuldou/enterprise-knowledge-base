package org.example.aa.service;

import org.example.aa.entity.DocumentChunk;
import org.example.aa.mapper.DocumentChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private static final double RRF_K = 60.0;

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
        // 语义通道：向量检索
        float[] queryVector = embeddingService.embed(query);
        String queryVectorStr = jsonMapper.writeValueAsString(queryVector);
        List<DocumentChunk> vectorHits = chunkMapper.search(queryVectorStr, topK);

        // 词法通道：关键词检索
        List<DocumentChunk> keywordHits = chunkMapper.searchByKeyword(query, topK);

        // RRF 融合
        List<RetrievedChunk> result = rrfFusion(vectorHits, keywordHits, topK);
        if (!result.isEmpty()) {
            log.info("混合检索完成: 向量 {} 条 + 关键词 {} 条 → 融合 {} 条",
                    vectorHits.size(), keywordHits.size(), result.size());
        }
        return result;
    }

    private List<RetrievedChunk> rrfFusion(List<DocumentChunk> vectorHits,
                                           List<DocumentChunk> keywordHits,
                                           int topK) {
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, RetrievedChunk> chunks = new LinkedHashMap<>();

        accumulate(vectorHits, rrfScores, chunks);
        accumulate(keywordHits, rrfScores, chunks);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> chunks.get(e.getKey()))
                .toList();
    }

    private void accumulate(List<DocumentChunk> hits,
                            Map<String, Double> rrfScores,
                            Map<String, RetrievedChunk> chunks) {
        for (int i = 0; i < hits.size(); i++) {
            DocumentChunk c = hits.get(i);
            String key = c.getDocumentId() + ":" + c.getChunkIndex();
            rrfScores.merge(key, 1.0 / (RRF_K + i + 1), Double::sum);
            chunks.putIfAbsent(key, new RetrievedChunk(
                    c.getDocumentId(), c.getChunkIndex(), c.getContent(), c.getSimilarity()));
        }
    }
}
