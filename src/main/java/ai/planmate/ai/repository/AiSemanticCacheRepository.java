package ai.planmate.ai.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pgvector.PGvector;

import ai.planmate.ai.entity.AiSemanticCache;

/**
 * Repository for AI Semantic Cache with pgvector similarity search.
 *
 * <p><b>KEY QUERY:</b> Vector similarity search using cosine distance.
 *
 * <p><b>PERFORMANCE:</b> Uses HNSW index (idx_ai_semantic_cache_vector_hnsw) for fast approximate
 * nearest neighbor search.
 */
@Repository
public interface AiSemanticCacheRepository extends JpaRepository<AiSemanticCache, UUID> {

    /**
     * Find similar prompts using vector similarity search.
     *
     * <p><b>QUERY LOGIC:</b>
     *
     * <ul>
     *   <li>Cosine similarity >= threshold (default: 0.95)
     *   <li>Not expired (expires_at > now)
     *   <li>Ordered by similarity (most similar first)
     * </ul>
     *
     * <p><b>OPERATOR:</b>
     *
     * <ul>
     *   <li>{@code <=>} = Cosine distance (0 = identical, 2 = opposite)
     *   <li>{@code 1 - <=>} = Cosine similarity (1 = identical, -1 = opposite)
     * </ul>
     *
     * <p><b>PERFORMANCE:</b> Uses HNSW index for O(log n) search instead of O(n).
     *
     * <p><b>EXAMPLE:</b>
     *
     * <pre>
     * var embedding = openAiService.generateEmbedding("Plan an e-commerce project");
     * var similarCaches = repository.findSimilarPrompts(
     *     new PGvector(embedding),
     *     0.95,
     *     5
     * );
     * </pre>
     *
     * @param promptEmbedding The query embedding (1536 dimensions)
     * @param similarityThreshold Minimum cosine similarity (default: 0.95)
     * @param maxResults Maximum number of results (default: 5)
     * @return List of similar cache entries, ordered by similarity
     */
    @Query(
            value =
                    """
        SELECT *,
               1 - (prompt_embedding <=> CAST(:promptEmbedding AS vector)) AS cosine_similarity
        FROM ai_semantic_cache
        WHERE expires_at > CURRENT_TIMESTAMP
        AND (1 - (prompt_embedding <=> CAST(:promptEmbedding AS vector))) >= :similarityThreshold
        ORDER BY prompt_embedding <=> CAST(:promptEmbedding AS vector)
        LIMIT :maxResults
    """,
            nativeQuery = true)
    List<AiSemanticCache> findSimilarPrompts(
            @Param("promptEmbedding") PGvector promptEmbedding,
            @Param("similarityThreshold") double similarityThreshold,
            @Param("maxResults") int maxResults);

    /**
     * Find the most similar prompt (convenience method).
     *
     * <p>Returns the single most similar cache entry, if one exists.
     *
     * @param promptEmbedding The query embedding
     * @param similarityThreshold Minimum cosine similarity (default: 0.95)
     * @return Optional containing the most similar cache entry
     */
    default Optional<AiSemanticCache> findMostSimilarPrompt(
            PGvector promptEmbedding, double similarityThreshold) {
        var results = findSimilarPrompts(promptEmbedding, similarityThreshold, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find cache entries by request type (for analytics).
     *
     * @param requestType The request type (e.g., "SPRINT_PLANNING")
     * @return List of cache entries
     */
    List<AiSemanticCache> findByRequestType(String requestType);

    /**
     * Find expired cache entries for cleanup.
     *
     * @param now Current timestamp
     * @return List of expired cache entries
     */
    @Query("SELECT c FROM AiSemanticCache c WHERE c.expiresAt <= :now ORDER BY c.createdAt ASC")
    List<AiSemanticCache> findExpiredEntries(@Param("now") Instant now);

    /**
     * Count active (non-expired) cache entries.
     *
     * @return Number of active cache entries
     */
    @Query("SELECT COUNT(c) FROM AiSemanticCache c WHERE c.expiresAt > CURRENT_TIMESTAMP")
    long countActive();

    /**
     * Calculate total cost savings from cache hits.
     *
     * <p>Formula: SUM(hit_count) × (GPT_cost - embedding_cost)
     *
     * <p>Assumes:
     *
     * <ul>
     *   <li>GPT-4 call: $0.002
     *   <li>Embedding call: $0.0001
     * </ul>
     *
     * @return Total cost savings in dollars
     */
    @Query(
            """
        SELECT COALESCE(SUM(c.hitCount), 0) * (0.002 - 0.0001)
        FROM AiSemanticCache c
        WHERE c.expiresAt > CURRENT_TIMESTAMP
    """)
    double calculateTotalCostSavings();

    /**
     * Find most frequently used cache entries (for monitoring).
     *
     * @param limit Maximum number of results
     * @return List of cache entries ordered by hit count
     */
    @Query(
            """
        SELECT c FROM AiSemanticCache c
        WHERE c.expiresAt > CURRENT_TIMESTAMP
        AND c.hitCount > 0
        ORDER BY c.hitCount DESC
    """)
    List<AiSemanticCache> findTopCacheHits(@Param("limit") int limit);

    /**
     * Delete expired cache entries (cleanup job).
     *
     * @param now Current timestamp
     * @return Number of deleted entries
     */
    @Modifying
    @Query("DELETE FROM AiSemanticCache c WHERE c.expiresAt <= :now")
    int deleteExpiredEntries(@Param("now") Instant now);
}
