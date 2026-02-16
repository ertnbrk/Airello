package ai.planmate.agile.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.agile.service.PlanningEngineService;
import ai.planmate.ai.dto.PlanningAutoResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/projects/{projectId}/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningEngineService planningEngineService;

    @PostMapping("/auto")
    public PlanningAutoResponse autoPlanning(@PathVariable UUID projectId) {
        return planningEngineService.autoPlanningForFrontend(projectId);
    }
}
