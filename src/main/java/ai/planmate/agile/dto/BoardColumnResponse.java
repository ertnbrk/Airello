package ai.planmate.agile.dto;

import java.util.List;
import java.util.UUID;

import ai.planmate.agile.entity.ColumnCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardColumnResponse {
    private UUID id;
    private String name;
    private Integer position;
    private Boolean isDefault;
    private ColumnCategory category;
    private Integer wipLimit;
    private List<IssueSummaryDto> issues;
}
