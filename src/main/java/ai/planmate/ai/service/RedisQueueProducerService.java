package ai.planmate.ai.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis Queue Producer Service
 *
 * <p>Enqueues AI processing jobs to Redis LIST for consumption by Python AI worker.
 *
 * <p><b>Architecture:</b>
 *
 * <pre>
 * Java API:        LPUSH ai:jobs {json}
 * Python Worker:   BRPOP ai:jobs (blocking pop)
 * </pre>
 *
 * <p><b>Job Payload Schema:</b>
 *
 * <pre>
 * {
 *   "correlationId": "uuid",
 *   "projectId": "uuid",
 *   "userId": "uuid",
 *   "requestType": "SPRINT_PLANNING|ISSUE_ESTIMATION|etc",
 *   "language": "en",
 *   "prompt": "Plan an e-commerce project",
 *   "context": { ... project snapshot ... },
 *   "constraints": {
 *     "maxTokens": 2000,
 *     "temperature": 0.7
 *   }
 * }
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "planmate.features.redis-enabled", havingValue = "true")
public class RedisQueueProducerService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.queue.key:ai:jobs}")
    private String queueKey;

    /**
     * Enqueue an AI job for processing.
     *
     * @param correlationId Unique correlation ID
     * @param projectId Project UUID
     * @param userId User UUID (nullable)
     * @param requestType Type of AI request
     * @param prompt User's prompt
     * @param context Optional project context
     * @param constraints Processing constraints (maxTokens, temperature)
     */
    public void enqueueJob(
            String correlationId,
            UUID projectId,
            UUID userId,
            String requestType,
            String prompt,
            Map<String, Object> context,
            Map<String, Object> constraints) {

        try {
            Map<String, Object> job = new HashMap<>();
            job.put("correlationId", correlationId);
            job.put("projectId", projectId.toString());
            if (userId != null) {
                job.put("userId", userId.toString());
            }
            job.put("requestType", requestType);
            job.put("language", "en"); // TODO: Make configurable
            job.put("prompt", prompt);
            job.put("context", context != null ? context : Map.of());
            job.put("constraints", constraints != null ? constraints : getDefaultConstraints());

            String jobJson = objectMapper.writeValueAsString(job);

            // LPUSH adds to the left (head) of the list
            // Python worker does BRPOP from the right (tail) - FIFO order
            redisTemplate.opsForList().leftPush(queueKey, jobJson);

            log.info(
                    "✅ Enqueued AI job: correlationId={ }, requestType={ }, queueKey={ }",
                    correlationId,
                    requestType,
                    queueKey);

        } catch (Exception e) {
            log.error("❌ Failed to enqueue AI job: correlationId={ }", correlationId, e);
            throw new RuntimeException("Failed to enqueue AI job", e);
        }
    }

    /** Get the current queue length (number of pending jobs). */
    public long getQueueLength() {
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null ? size : 0;
    }

    private Map<String, Object> getDefaultConstraints() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("maxTokens", 2000);
        defaults.put("temperature", 0.7);
        return defaults;
    }
}
