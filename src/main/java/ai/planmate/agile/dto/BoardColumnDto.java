package ai.planmate.agile.dto;

import java.util.UUID;

import ai.planmate.agile.entity.BoardColumn;
import ai.planmate.agile.entity.ColumnCategory;

/**
 * DTO for BoardColumn entity - safe for WebSocket/STOMP serialization. Contains only
 * primitive/simple fields, no entity references or lazy proxies.
 */
public record BoardColumnDto(
        UUID id,
        String name,
        Integer position,
        Boolean isDefault,
        ColumnCategory category,
        Integer wipLimit,
        UUID projectId) {

    /**
     * Creates a DTO from a BoardColumn entity. Extracts only projectId without triggering lazy
     * proxy initialization.
     *
     * @param column the BoardColumn entity
     * @return BoardColumnDto with minimal data
     */
    public static BoardColumnDto fromEntity(BoardColumn column) {
        return new BoardColumnDto(
                column.getId(),
                column.getName(),
                column.getPosition(),
                column.getIsDefault(),
                column.getCategory(),
                column.getWipLimit(),
                column.getProject().getId());
    }
}
