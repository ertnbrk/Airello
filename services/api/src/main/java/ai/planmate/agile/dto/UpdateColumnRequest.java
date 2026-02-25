package ai.planmate.agile.dto;

import ai.planmate.agile.entity.ColumnCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateColumnRequest {
    @Size(min = 1, max = 60, message = "Column name must be between 1 and 60 characters")
    private String name;

    private ColumnCategory category;

    @Min(value = 1, message = "WIP limit must be at least 1 if specified")
    private Integer wipLimit;

    private Boolean isDefault;
}
