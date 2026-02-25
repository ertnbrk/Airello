package ai.planmate.ai.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.ai.dto.AiCallbackDto;
import ai.planmate.ai.dto.AiRequestResponse;
import ai.planmate.ai.dto.StartAiRequestDto;
import ai.planmate.ai.mapper.AiRequestMapper;
import ai.planmate.ai.service.AiCallbackService;
import ai.planmate.ai.service.AiOrchestrationService;
import ai.planmate.auth.entity.AppUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AI Controller
 *
 * <p>Handles AI request lifecycle:
 *
 * <ul>
 *   <li>POST /v1/projects/{id}/ai/start - Start AI processing (enqueue to Redis)
 *   <li>GET /v1/ai/requests/{correlationId} - Check request status
 *   <li>POST /v1/ai/callback - Internal callback from Python worker
 * </ul>
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AiController {

    private final AiOrchestrationService orchestrationService;
    private final AiCallbackService callbackService;

    @PostMapping("/projects/{projectId}/ai/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AiRequestResponse startAiRequest(
            @PathVariable UUID projectId,
            @Valid @RequestBody StartAiRequestDto request,
            Authentication authentication) {
        request.setProjectId(projectId);

        // Extract current user from Spring Security context
        AppUser currentUser = null;
        if (authentication != null && authentication.getPrincipal() instanceof AppUser) {
            currentUser = (AppUser) authentication.getPrincipal();
        }

        return AiRequestMapper.toResponse(
                orchestrationService.startAiRequest(request, currentUser));
    }

    @GetMapping("/ai/requests/{correlationId}")
    public AiRequestResponse getRequestStatus(@PathVariable String correlationId) {
        return AiRequestMapper.toResponse(orchestrationService.getRequestStatus(correlationId));
    }

    @PostMapping("/ai/callback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleAiCallback(@Valid @RequestBody AiCallbackDto callback) {
        callbackService.handleCallback(callback);
    }
}
