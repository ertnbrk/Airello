package ai.planmate.agile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateColumnRequest {
    @NotBlank(message = "Column name is required")
    @Size(max = 100, message = "Column name must not exceed 100 characters")
    private String name;
}
