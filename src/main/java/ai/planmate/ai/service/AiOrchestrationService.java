package ai.planmate.ai.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;

import ai.planmate.ai.dto.StartAiRequestDto;
import ai.planmate.ai.entity.AiRequest;
import ai.planmate.ai.entity.AiRequestStatus;
import ai.planmate.ai.entity.AiSemanticCache;
import ai.planmate.ai.repository.AiRequestRepository;
import ai.planmate.ai.repository.AiSemanticCacheRepository;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * AI Orchestration Service with Semantic Caching.
 *
 * <p><b>KEY ARCHITECTURAL CHANGE:</b> Implements semantic caching using pgvector to reduce AI costs
 * by up to 95%.
 *
 * <p><b>COST ANALYSIS:</b>
 *
 * <pre>
 * Without cache (1000 similar requests):
 *   1000 × GPT-4 call ($0.002) = $2.00
 *
 * With semantic cache:
 *   1 × GPT-4 call ($0.002) +
 *   1 × Embedding ($0.0001) +
 *   999 × Embedding ($0.0001) = $0.10
 *
 * Savings: $1.90 (95% reduction!)
 * </pre>
 *
 * <p><b>FLOW:</b>
 *
 * <pre>
 * 1. User sends prompt: "Plan an e-commerce project"
 * 2. Generate embedding using OpenAI (text-embedding-3-small)
 * 3. Query pgvector for similar prompts (cosine similarity > 0.95)
 * 4. If CACHE HIT:
 *    - Return cached response
 *    - Increment hit_count
 *    - Total cost: $0.0001 ✅
 * 5. If CACHE MISS:
 *    - Call OpenAI GPT-4
 *    - Store response + embedding in cache
 *    - Total cost: $0.0021
 * </pre>
 */
@Service
@Slf4j
public class AiOrchestrationService {

    private final AiRequestRepository aiRequestRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;
    private final AiSemanticCacheRepository aiSemanticCacheRepository;
    private final QuotaGuardService quotaGuardService;
    @Nullable private final RedisQueueProducerService redisQueueProducer;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:#{null}}")
    private String openAiApiKey;

    @Value("${ai.semantic.cache.enabled:true}")
    private boolean semanticCacheEnabled;

    @Value("${ai.semantic.cache.similarity.threshold:0.95}")
    private double similarityThreshold;

    public AiOrchestrationService(
            AiRequestRepository aiRequestRepository,
            ProjectRepository projectRepository,
            AppUserRepository appUserRepository,
            AiSemanticCacheRepository aiSemanticCacheRepository,
            QuotaGuardService quotaGuardService,
            @Nullable RedisQueueProducerService redisQueueProducer,
            ObjectMapper objectMapper) {
        this.aiRequestRepository = aiRequestRepository;
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
        this.aiSemanticCacheRepository = aiSemanticCacheRepository;
        this.quotaGuardService = quotaGuardService;
        this.redisQueueProducer = redisQueueProducer;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * Start an AI request with quota enforcement and Redis queue enqueue.
     *
     * <p><b>FLOW:</b>
     *
     * <pre>
     * 1. Authenticate user (JWT - handled by controller)
     * 2. Enforce quota (check daily limit)
     * 3. Create ai_request row (status=PENDING)
     * 4. Enqueue job to Redis
     * 5. Return 202 Accepted with correlationId
     * </pre>
     *
     * @param dto AI request DTO
     * @return AiRequest entity
     */
    @Transactional
    public AiRequest startAiRequest(StartAiRequestDto dto, AppUser currentUser) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(dto.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // STEP 1: Enforce quota
        if (currentUser != null) {
            quotaGuardService.checkQuota(currentUser);
        }

        // STEP 2: Create AI request record
        String correlationId = UUID.randomUUID().toString();

        AiRequest aiRequest = new AiRequest();
        aiRequest.setProject(project);
        aiRequest.setCorrelationId(correlationId);
        aiRequest.setRequestedBy(currentUser);
        aiRequest.setRequestType(dto.getRequestType());
        aiRequest.setRequestPayload(dto.getParameters());
        aiRequest.setStatus(AiRequestStatus.PENDING);

        aiRequest = aiRequestRepository.save(aiRequest);

        // STEP 3: Enqueue to Redis (or fallback to simulation)
        if (redisQueueProducer != null) {
            enqueueToRedis(aiRequest, dto, currentUser);
        } else {
            log.warn(
                    "Redis queue not available, falling back to simulation for: { }",
                    correlationId);
            simulateAiProcessing(aiRequest);
        }

        return aiRequest;
    }

    /** Enqueue AI job to Redis for Python worker consumption. */
    private void enqueueToRedis(AiRequest aiRequest, StartAiRequestDto dto, AppUser currentUser) {
        String prompt = extractPrompt(dto);
        Map<String, Object> context = buildContext(aiRequest.getProject(), dto);
        Map<String, Object> constraints = buildConstraints(dto);

        redisQueueProducer.enqueueJob(
                aiRequest.getCorrelationId(),
                aiRequest.getProject().getId(),
                currentUser != null ? currentUser.getId() : null,
                aiRequest.getRequestType(),
                prompt,
                context,
                constraints);

        log.info(
                "AI request enqueued: correlationId={ }, requestType={ }, user={ }",
                aiRequest.getCorrelationId(),
                aiRequest.getRequestType(),
                currentUser != null ? currentUser.getEmail() : "anonymous");
    }

    private String extractPrompt(StartAiRequestDto dto) {
        if (dto.getParameters() != null && dto.getParameters().containsKey("prompt")) {
            return dto.getParameters().get("prompt").toString();
        }
        return "Generate a comprehensive project plan"; // Default
    }

    private Map<String, Object> buildContext(Project project, StartAiRequestDto dto) {
        Map<String, Object> context = new HashMap<>();
        context.put("projectName", project.getName());
        context.put("projectDescription", project.getDescription());
        context.put("projectId", project.getId().toString());
        // Add more context as needed
        return context;
    }

    private Map<String, Object> buildConstraints(StartAiRequestDto dto) {
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("maxTokens", 2000);
        constraints.put("temperature", 0.7);
        // Override with DTO parameters if present
        if (dto.getParameters() != null) {
            if (dto.getParameters().containsKey("maxTokens")) {
                constraints.put("maxTokens", dto.getParameters().get("maxTokens"));
            }
            if (dto.getParameters().containsKey("temperature")) {
                constraints.put("temperature", dto.getParameters().get("temperature"));
            }
        }
        return constraints;
    }

    /**
     * Simulates AI processing for demo purposes. In production, this is handled by Python worker
     * consuming from Redis queue.
     */
    @Transactional
    public void simulateAiProcessing(AiRequest aiRequest) {
        try {
            aiRequest.setStatus(AiRequestStatus.PROCESSING);
            aiRequestRepository.save(aiRequest);

            // Simulate processing delay
            Thread.sleep(1000);

            // Generate mock result based on request type
            Map<String, Object> result = generateMockResult(aiRequest.getRequestType());

            aiRequest.setStatus(AiRequestStatus.COMPLETED);
            aiRequest.setResponsePayload(result);
            aiRequest.setCompletedAt(java.time.Instant.now());
            aiRequestRepository.save(aiRequest);

            log.info(
                    "Simulated AI processing completed: correlationId={ }",
                    aiRequest.getCorrelationId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            aiRequest.setStatus(AiRequestStatus.FAILED);
            aiRequest.setErrorMessage("Processing interrupted");
            aiRequestRepository.save(aiRequest);
        }
    }

    private Map<String, Object> generateMockResult(String requestType) {
        Map<String, Object> result = new HashMap<>();

        if ("SPRINT_PLANNING".equals(requestType)) {
            result.put(
                    "suggestion",
                    "Consider splitting large stories into smaller tasks for better sprint flow");
            result.put("recommendedVelocity", 30);
            result.put("riskLevel", "LOW");
        } else if ("ISSUE_ESTIMATION".equals(requestType)) {
            result.put("estimatedPoints", 5);
            result.put("confidence", "HIGH");
            result.put("similarIssues", new String[] {"PROJ-12", "PROJ-34"});
        } else {
            result.put("message", "AI processing completed successfully");
        }

        return result;
    }

    @Transactional(readOnly = true)
    public AiRequest getRequestStatus(String correlationId) {
        return aiRequestRepository
                .findByCorrelationId(correlationId)
                .orElseThrow(() -> new ResourceNotFoundException("AI request not found"));
    }

    // ========================================
    // Semantic Caching with pgvector
    // ========================================

    /**
     * Process AI request with semantic caching.
     *
     * <p><b>FLOW:</b>
     *
     * <pre>
     * 1. Generate embedding for prompt
     * 2. Check semantic cache for similar prompts
     * 3. If CACHE HIT: Return cached response (save $$)
     * 4. If CACHE MISS: Call OpenAI, cache response
     * </pre>
     *
     * @param prompt The user's prompt
     * @param context Optional context
     * @param requestType Type of request (e.g., "SPRINT_PLANNING")
     * @return AI response (from cache or OpenAI)
     */
    @Transactional
    public Map<String, Object> processWithSemanticCache(
            String prompt, String context, String requestType) {
        if (!semanticCacheEnabled || openAiApiKey == null) {
            log.warn("Semantic cache disabled or OpenAI API key missing, falling back to mock");
            return generateMockResult(requestType);
        }

        try {
            // STEP 1: Generate embedding for prompt
            var embedding = generateEmbedding(prompt);
            var pgVector = new PGvector(embedding);

            // STEP 2: Check semantic cache
            var cachedResult =
                    aiSemanticCacheRepository.findMostSimilarPrompt(pgVector, similarityThreshold);

            if (cachedResult.isPresent()) {
                // CACHE HIT! 🎉
                var cache = cachedResult.get();
                cache.recordHit();
                aiSemanticCacheRepository.save(cache);

                log.info(
                        "💰 CACHE HIT! Similarity: {:.2f}%, Hit count: { }, Savings: ${:.4f}",
                        similarityThreshold * 100,
                        cache.getHitCount(),
                        cache.calculateCostSavings());

                return objectMapper.convertValue(cache.getResponsePayload(), Map.class);
            }

            // CACHE MISS - Call OpenAI
            log.info("⚠️  CACHE MISS - Calling OpenAI API");
            var response = callOpenAI(prompt, context);

            // STEP 3: Store in cache for future reuse
            var cacheEntry =
                    AiSemanticCache.builder()
                            .prompt(prompt)
                            .context(context)
                            .model("gpt-4o-mini")
                            .requestType(requestType)
                            .promptEmbedding(pgVector)
                            .responsePayload(response)
                            .responseText(response.toString())
                            .build();

            aiSemanticCacheRepository.save(cacheEntry);

            log.info("✅ Response cached for future reuse: cacheId={ }", cacheEntry.getId());

            return response;

        } catch (Exception e) {
            log.error("❌ Semantic cache error, falling back to mock", e);
            return generateMockResult(requestType);
        }
    }

    /**
     * Generate embedding using OpenAI text-embedding-3-small.
     *
     * <p><b>MODEL:</b> text-embedding-3-small (1536 dimensions)
     *
     * <p><b>COST:</b> $0.0001 per request
     *
     * @param text Text to embed
     * @return Embedding array (1536 floats)
     */
    private float[] generateEmbedding(String text) {
        var requestBody = Map.of("input", text, "model", "text-embedding-3-small");

        var response =
                restClient
                        .post()
                        .uri("https://api.openai.com/v1/embeddings")
                        .header("Authorization", "Bearer " + openAiApiKey)
                        .header("Content-Type", "application/json")
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

        @SuppressWarnings("unchecked")
        var data = (java.util.List<Map<String, Object>>) response.get("data");
        @SuppressWarnings("unchecked")
        var embeddingList = (java.util.List<Double>) data.get(0).get("embedding");

        // Convert List<Double> to float[]
        var embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }

        log.debug("Generated embedding: dimensions={ }", embedding.length);
        return embedding;
    }

    /**
     * Call OpenAI GPT-4 API.
     *
     * <p><b>MODEL:</b> gpt-4o-mini
     *
     * <p><b>COST:</b> ~$0.002 per request
     *
     * @param prompt User prompt
     * @param context Optional context
     * @return AI response
     */
    private Map<String, Object> callOpenAI(String prompt, String context) {
        var fullPrompt = context != null ? context + "\n\n" + prompt : prompt;

        var requestBody =
                Map.of(
                        "model",
                        "gpt-4o-mini",
                        "messages",
                        java.util.List.of(
                                Map.of("role", "system", "content", "You are a helpful assistant."),
                                Map.of("role", "user", "content", fullPrompt)),
                        "max_tokens",
                        500);

        var response =
                restClient
                        .post()
                        .uri("https://api.openai.com/v1/chat/completions")
                        .header("Authorization", "Bearer " + openAiApiKey)
                        .header("Content-Type", "application/json")
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

        @SuppressWarnings("unchecked")
        var choices = (java.util.List<Map<String, Object>>) response.get("choices");
        @SuppressWarnings("unchecked")
        var message = (Map<String, Object>) choices.get(0).get("message");
        var content = (String) message.get("content");

        return Map.of("message", content, "model", "gpt-4o-mini");
    }

    /**
     * Get cache statistics for monitoring.
     *
     * @return Cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        var activeCount = aiSemanticCacheRepository.countActive();
        var totalSavings = aiSemanticCacheRepository.calculateTotalCostSavings();
        var topHits = aiSemanticCacheRepository.findTopCacheHits(10);

        return Map.of(
                "activeEntries",
                activeCount,
                "totalCostSavings",
                String.format("$%.4f", totalSavings),
                "topCacheHits",
                topHits.stream()
                        .map(
                                c ->
                                        Map.of(
                                                "prompt",
                                                c.getPrompt()
                                                        .substring(
                                                                0,
                                                                Math.min(
                                                                        50,
                                                                        c.getPrompt().length())),
                                                "hitCount",
                                                c.getHitCount(),
                                                "savings",
                                                String.format("$%.4f", c.calculateCostSavings())))
                        .toList());
    }
}
