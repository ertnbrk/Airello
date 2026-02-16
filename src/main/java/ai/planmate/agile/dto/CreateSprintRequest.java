package ai.planmate.agile.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSprintRequest {
    @NotNull private UUID projectId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String goal;

    @NotNull private LocalDate startDate;

    @NotNull private LocalDate endDate;
}
