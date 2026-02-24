package ai.planmate.agile.dto;

import java.time.Instant;
import java.util.UUID;

import ai.planmate.agile.entity.Epic;
import ai.planmate.agile.mapper.EnumMapper;

/**
 * DTO for Epic entity - safe for REST/WebSocket serialization. Contains only primitive/simple
 * fields, no entity references or lazy proxies.
 */
public record EpicDto(
        UUID id,
        UUID projectId,
        String title,
        String description,
        String key,
        String priority, // Frontend format: 'low', 'medium', 'high', 'urgent'
        String status, // Frontend format: 'backlog', 'todo', 'in-progress', 'review', 'done'
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Creates a DTO from an Epic entity. Extracts only IDs without triggering lazy proxy
     * initialization.
     *
     * @param epic the Epic entity
     * @return EpicDto with minimal data
     */
    public static EpicDto fromEntity(Epic epic) {
        return new EpicDto(
                epic.getId(),
                epic.getProject().getId(), // Safe: getId() doesn't trigger proxy init
                epic.getTitle(),
                epic.getDescription(),
                epic.getKey(),
                EnumMapper.toFrontendPriority(epic.getPriority()),
                EnumMapper.toFrontendStatus(epic.getStatus()),
                epic.getCreatedBy().getId(), // Safe: getId() doesn't trigger proxy init
                epic.getCreatedAt(),
                epic.getUpdatedAt());
    }
}
