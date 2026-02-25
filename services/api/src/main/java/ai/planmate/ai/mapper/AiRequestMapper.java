package ai.planmate.ai.mapper;

import ai.planmate.ai.dto.AiRequestResponse;
import ai.planmate.ai.entity.AiRequest;

public final class AiRequestMapper {

    private AiRequestMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static AiRequestResponse toResponse(AiRequest request) {
        if (request == null) {
            return null;
        }

        return AiRequestResponse.builder()
                .id(request.getId() != null ? request.getId().toString() : null)
                .correlationId(request.getCorrelationId())
                .projectId(
                        request.getProject() != null
                                ? request.getProject().getId().toString()
                                : null)
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .requestType(request.getRequestType())
                .parameters(request.getRequestPayload())
                .result(request.getResponsePayload())
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .build();
    }
}
