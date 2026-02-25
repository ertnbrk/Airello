package ai.planmate.ai.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.ai.dto.AiCallbackDto;
import ai.planmate.ai.entity.AiRequest;
import ai.planmate.ai.entity.AiRequestStatus;
import ai.planmate.ai.repository.AiRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI Callback Service
 *
 * <p>Handles callbacks from the Python AI worker after job completion.
 *
 * <p><b>IDEMPOTENCY:</b> Multiple callbacks with same correlationId are safe. If request is already
 * COMPLETED or FAILED, callback is ignored.
 *
 * <p><b>QUOTA RECORDING:</b> On successful completion, records token usage for quota enforcement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiCallbackService {

    private final AiRequestRepository aiRequestRepository;
    private final QuotaGuardService quotaGuardService;

    @Transactional
    public void handleCallback(AiCallbackDto callback) {
        AiRequest aiRequest =
                aiRequestRepository
                        .findByCorrelationId(callback.getCorrelationId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "AI request not found: "
                                                        + callback.getCorrelationId()));

        // Idempotency: skip if already completed
        if (aiRequest.getStatus() == AiRequestStatus.COMPLETED
                || aiRequest.getStatus() == AiRequestStatus.FAILED) {
            log.info("Skipping duplicate callback for { }", callback.getCorrelationId());
            return;
        }

        if ("COMPLETED".equals(callback.getStatus())) {
            processSuccessfulCallback(aiRequest, callback);
        } else {
            processFailedCallback(aiRequest, callback);
        }

        aiRequestRepository.save(aiRequest);
    }

    private void processSuccessfulCallback(AiRequest aiRequest, AiCallbackDto callback) {
        aiRequest.setStatus(AiRequestStatus.COMPLETED);
        aiRequest.setCompletedAt(Instant.now());

        Map<String, Object> response = new HashMap<>();
        response.put("epics", callback.getEpics());
        response.put("issues", callback.getIssues());
        response.put("sprints", callback.getSprints());
        aiRequest.setResponsePayload(response);

        // Record provider and token usage (from callback metadata)
        if (callback.getProvider() != null) {
            aiRequest.setProvider(callback.getProvider());
        }
        if (callback.getTokensUsed() != null) {
            aiRequest.setTokensUsed(callback.getTokensUsed());

            // Record usage for quota tracking
            if (aiRequest.getRequestedBy() != null) {
                quotaGuardService.recordUsage(aiRequest.getRequestedBy(), callback.getTokensUsed());
            }
        }

        log.info(
                "✅ AI request completed: correlationId={ }, provider={ }, tokens={ }",
                aiRequest.getCorrelationId(),
                aiRequest.getProvider(),
                aiRequest.getTokensUsed());
    }

    private void processFailedCallback(AiRequest aiRequest, AiCallbackDto callback) {
        aiRequest.setStatus(AiRequestStatus.FAILED);
        aiRequest.setCompletedAt(Instant.now());
        aiRequest.setErrorMessage(callback.getErrorMessage());

        log.error("AI request failed: { }", aiRequest.getCorrelationId());
    }
}
