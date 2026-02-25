package ai.planmate.projects.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.planmate.projects.entity.ArtifactChunk;

@Repository
public interface ArtifactChunkRepository extends JpaRepository<ArtifactChunk, UUID> {

    @Query(
            "SELECT ac FROM ArtifactChunk ac WHERE ac.artifact.id = :artifactId ORDER BY"
                    + " ac.chunkIndex")
    List<ArtifactChunk> findByArtifactIdOrderByChunkIndex(UUID artifactId);

    /**
     * Find chunks similar to the given embedding using pgvector cosine similarity. The embedding
     * parameter should be a vector string in pgvector format: '[0.1, 0.2, ...]'
     */
    @Query(
            value =
                    "SELECT * FROM artifact_chunk WHERE artifact_id IN (SELECT id FROM artifact"
                            + " WHERE project_id = :projectId AND deleted_at IS NULL) ORDER BY"
                            + " embedding <-> CAST(:embedding AS vector) LIMIT :limit",
            nativeQuery = true)
    List<ArtifactChunk> findSimilarChunks(
            @Param("projectId") UUID projectId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);
}
