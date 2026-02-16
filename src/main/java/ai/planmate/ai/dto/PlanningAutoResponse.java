package ai.planmate.ai.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningAutoResponse {
    private List<PlannedSprint> sprints;
    private String summary;
    private List<String> criticalPath; // Issue IDs

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlannedSprint {
        private Integer sprintNumber;
        private List<String> issues; // Issue IDs
        private Integer totalPoints;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
