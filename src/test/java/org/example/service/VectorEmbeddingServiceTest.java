package org.example.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class VectorEmbeddingServiceTest {

    private final VectorEmbeddingService vectorEmbeddingService = new VectorEmbeddingService();

    @Test
    void shouldCalculateCosineSimilarityForSameDirectionVectors() {
        float similarity = vectorEmbeddingService.calculateCosineSimilarity(
                List.of(1.0f, 2.0f, 3.0f),
                List.of(2.0f, 4.0f, 6.0f)
        );

        Assertions.assertEquals(1.0f, similarity, 0.0001f, "同方向向量的余弦相似度应接近 1");
    }

    @Test
    void shouldCalculateCosineSimilarityForOrthogonalVectors() {
        float similarity = vectorEmbeddingService.calculateCosineSimilarity(
                List.of(1.0f, 0.0f),
                List.of(0.0f, 1.0f)
        );

        Assertions.assertEquals(0.0f, similarity, 0.0001f, "正交向量的余弦相似度应为 0");
    }

    @Test
    void shouldRejectVectorsWithDifferentDimensions() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> vectorEmbeddingService.calculateCosineSimilarity(List.of(1.0f), List.of(1.0f, 2.0f)));

        Assertions.assertEquals("向量维度不匹配", exception.getMessage(), "维度不一致应抛出明确异常");
    }

    @Test
    void shouldReturnEmptyList_whenBatchContentIsEmpty() {
        List<List<Float>> embeddings = vectorEmbeddingService.generateEmbeddings(List.of());

        Assertions.assertTrue(embeddings.isEmpty(), "空批量输入应直接返回空列表，不调用外部 API");
    }
}
