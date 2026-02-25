package ai.planmate.ai.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiCallbackDto {
    @NotBlank(message = "Correlation ID is required")
    private String correlationId;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "COMPLETED|FAILED", message = "Status must be either COMPLETED or FAILED")
    private String status;

    @Valid private List<AiGeneratedEpic> epics;

    @Valid private List<AiGeneratedIssue> issues;

    @Valid private List<AiGeneratedSprint> sprints;

    @Size(max = 5000, message = "Error message must not exceed 5000 characters")
    private String errorMessage;

    @Size(max = 50, message = "Provider must not exceed 50 characters")
    private String provider; // "openai", "ollama", etc.

    @Min(value = 0, message = "Tokens used must be non-negative")
    private Integer tokensUsed;

    @Data
    public static class AiGeneratedEpic {
        @NotBlank(message = "Epic title is required")
        @Size(max = 500, message = "Epic title must not exceed 500 characters")
        private String title;

        @Size(max = 5000, message = "Epic description must not exceed 5000 characters")
        private String description;

        @Pattern(
                regexp = "LOW|MEDIUM|HIGH|URGENT",
                message = "Priority must be LOW, MEDIUM, HIGH, or URGENT")
        private String priority;
    }

    @Data
    public static class AiGeneratedIssue {
        @Size(max = 500, message = "Epic title must not exceed 500 characters")
        private String epicTitle;

        @NotBlank(message = "Issue title is required")
        @Size(max = 500, message = "Issue title must not exceed 500 characters")
        private String title;

        @Size(max = 5000, message = "Issue description must not exceed 5000 characters")
        private String description;

        @Pattern(regexp = "STORY|TASK|BUG|EPIC", message = "Type must be STORY, TASK, BUG, or EPIC")
        private String type;

        @Pattern(
                regexp = "LOW|MEDIUM|HIGH|URGENT",
                message = "Priority must be LOW, MEDIUM, HIGH, or URGENT")
        private String priority;

        @Min(value = 0, message = "Story points must be at least 0")
        @Max(value = 100, message = "Story points must not exceed 100")
        private Integer storyPoints;

        private List<String> labels;

        private List<String> dependsOn; // Issue titles this issue depends on
    }

    @Data
    public static class AiGeneratedSprint {
        @NotBlank(message = "Sprint name is required")
        @Size(max = 255, message = "Sprint name must not exceed 255 characters")
        private String name;

        @Size(max = 5000, message = "Sprint goal must not exceed 5000 characters")
        private String goal;

        private List<String> issueKeys;
    }
}
