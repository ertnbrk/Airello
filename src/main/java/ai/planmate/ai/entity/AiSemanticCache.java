package ai.planmate.ai.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.pgvector.PGvector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI Semantic Cache Entity using pgvector for similarity search.
 *
 * <p><b>PURPOSE:</b> Reduce AI costs by caching similar prompts using vector embeddings.
 *
 * <p><b>HOW IT WORKS:</b>
 *
 * <pre>
 * 1. User sends prompt: "Plan an e-commerce project"
 * 2. Generate embedding using OpenAI (text-embedding-3-small): $0.0001
 * 3. Query cache for similar embeddings (Cosine Similarity > 0.95)
 * 4. If HIT:
 *    - Return cached response
 *    - Increment hit_count
 *    - Total cost: $0.0001 (95% savings!)
 * 5. If MISS:
 *    - Call OpenAI GPT-4: $0.002
 *    - Store response + embedding in cache
 *    - Total cost: $0.0021
 * </pre>
 *
 * <p><b>COST SAVINGS:</b>
 *
 * <pre>
 * Scenario: 1000 similar requests
 * Without cache: 1000 × $0.002 = $2.00
 * With cache: 1 × $0.002 + 999 × $0.0001 = $0.10 (95% savings!)
 * </pre>
 *
 * <p><b>EMBEDDING MODEL:</b> OpenAI text-embedding-3-small (1536 dimensions)
 *
 * <p><b>SIMILARITY THRESHOLD:</b> 0.95 (95% similar = cache hit)
 *
 * <p><b>TTL:</b> 7 days (configurable)
 */
@Entity
@Table(name = "ai_semantic_cache")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSemanticCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Original prompt text (for debugging and analytics). */
    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    /**
     * Optional context (e.g., project requirements, user preferences).
     *
     * <p>Context is included in the cache key to avoid false positives.
     */
    @Column(name = "context", columnDefinition = "TEXT")
    private String context;

    /**
     * AI model used to generate the response.
     *
     * <p>Example: "gpt-4o-mini", "gpt-4", "claude-3-sonnet"
     */
    @Column(name = "model", nullable = false, length = 100)
    private String model;

    /**
     * Type of AI request (for analytics and monitoring).
     *
     * <p>Example: "SPRINT_PLANNING", "ISSUE_ESTIMATION", "CODE_REVIEW"
     */
    @Column(name = "request_type", nullable = false, length = 100)
    private String requestType;

    /**
     * Vector embedding of the prompt (1536 dimensions).
     *
     * <p><b>IMPORTANT:</b> This is the key to semantic caching!
     *
     * <p>Similar prompts will have similar embeddings (cosine similarity close to 1.0).
     *
     * <p><b>PGVECTOR TYPE:</b> vector(1536)
     */
    @Column(name = "prompt_embedding", nullable = false, columnDefinition = "vector(1536)")
    private PGvector promptEmbedding;

    /**
     * Cached AI response (stored as JSONB).
     *
     * <p>Stored as JSON for flexibility (can store any response structure).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", nullable = false, columnDefinition = "jsonb")
    private Object responsePayload;

    /** Plain text response (for quick access without parsing JSON). */
    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    /**
     * Number of times this cache entry was reused.
     *
     * <p>Higher hit_count = popular cache entry = high cost savings!
     */
    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private Integer hitCount = 0;

    /** Last time this cache entry was hit (for cache analytics). */
    @Column(name = "last_hit_at")
    private Instant lastHitAt;

    /**
     * Cache expiration timestamp (TTL).
     *
     * <p>Default: 7 days from creation.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (expiresAt == null) {
            // Default TTL: 7 days
            expiresAt = now.plusSeconds(7L * 24 * 60 * 60);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Records a cache hit (increments hit_count and updates last_hit_at). */
    public void recordHit() {
        this.hitCount++;
        this.lastHitAt = Instant.now();
    }

    /**
     * Calculates cost savings from this cache entry.
     *
     * <p>Assumes:
     *
     * <ul>
     *   <li>GPT-4 call: $0.002
     *   <li>Embedding call: $0.0001
     * </ul>
     *
     * @return Cost savings in dollars
     */
    public double calculateCostSavings() {
        var gptCost = 0.002;
        var embeddingCost = 0.0001;
        return (hitCount * gptCost) - (hitCount * embeddingCost);
    }
}
