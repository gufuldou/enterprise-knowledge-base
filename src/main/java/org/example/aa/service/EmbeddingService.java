package org.example.aa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final RestClient restClient;
    private final String model;

    public EmbeddingService(
            @Value("${app.embedding.base-url}") String baseUrl,
            @Value("${app.embedding.api-key}") String apiKey,
            @Value("${app.embedding.model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * 批量向量化：调用 OpenAI 兼容的 /embeddings 接口，
     * 一次传入多个文本，返回对应的一维向量数组（float[]）。
     */
    public List<float[]> embedBatch(List<String> texts) {
        Map<String, Object> request = Map.of(
                "model", model,
                "input", texts,
                "encoding_format", "float"
        );

        Map<String, Object> response = restClient.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        List<?> data = (List<?>) response.get("data");
        return data.stream()
                .map(item -> toVector((List<?>) ((Map<?, ?>) item).get("embedding")))
                .toList();
    }

    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    private float[] toVector(List<?> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = ((Number) values.get(i)).floatValue();
        }
        return vector;
    }
}
