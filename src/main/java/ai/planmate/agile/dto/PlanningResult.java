package ai.planmate.agile.dto;

import java.util.List;

import ai.planmate.agile.service.PlanningEngineService;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanningResult {
    private List<PlanningEngineService.SprintPlan> sprints;
    private String summary;
    private List<String> criticalPath;
}
