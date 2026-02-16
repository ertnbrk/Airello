package ai.planmate.projects.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.planmate.projects.entity.Artifact;
import ai.planmate.projects.entity.ArtifactChunk;
import ai.planmate.projects.repository.ArtifactChunkRepository;
import ai.planmate.projects.repository.ArtifactRepository;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing embeddings and chunk storage. Embedding generation is done by external AI
 * service; this service only stores and queries embeddings.
 */
@Service
@ConditionalOnProperty(name = "planmate.features.artifacts-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final ArtifactChunkRepository chunkRepository;
    private final ArtifactRepository artifactRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void storeChunks(UUID artifactId, List<ChunkData> chunks) {
        Artifact artifact =
                artifactRepository
                        .findByIdAndNotDeleted(artifactId)
                        .orElseThrow(() -> new ResourceNotFoundException("Artifact not found"));

        for (int i = 0; i < chunks.size(); i++) {
            ChunkData data = chunks.get(i);
            ArtifactChunk chunk = new ArtifactChunk();
            chunk.setArtifact(artifact);
            chunk.setChunkIndex(i);
            chunk.setContent(data.getContent());
            chunk.setEmbedding(formatEmbedding(data.getEmbedding()));
            chunk.setMetadata(toJson(data.getMetadata()));
            chunkRepository.save(chunk);
        }

        log.info("Stored { } chunks for artifact { }", chunks.size(), artifactId);
    }

    @Transactional(readOnly = true)
    public List<ArtifactChunk> findSimilarChunks(UUID projectId, float[] embedding, int limit) {
        String embeddingStr = formatEmbedding(embedding);
        return chunkRepository.findSimilarChunks(projectId, embeddingStr, limit);
    }

    /** Convert float array to pgvector format: [0.1, 0.2, 0.3] */
    private String formatEmbedding(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
            return "{ }";
        }
    }

    public static class ChunkData {
        private String content;
        private float[] embedding;
        private Map<String, Object> metadata = new HashMap<>();

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public float[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
