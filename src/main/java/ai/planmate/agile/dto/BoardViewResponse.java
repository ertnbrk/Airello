package ai.planmate.agile.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Board view response with dynamic columns or legacy status-based columns. If dynamicColumns is
 * populated, use that. Otherwise fall back to columns map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardViewResponse {
    // Legacy: status-based grouping (backlog, todo, in-progress, review, done)
    private Map<String, List<IssueSummaryDto>> columns;

    // New: dynamic board columns with issues
    private List<BoardColumnResponse> dynamicColumns;
}
