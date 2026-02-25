package ai.planmate.agile.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.agile.dto.CreateSprintRequest;
import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.entity.Sprint;
import ai.planmate.agile.entity.SprintIssue;
import ai.planmate.agile.entity.SprintStatus;
import ai.planmate.agile.repository.IssueRepository;
import ai.planmate.agile.repository.SprintRepository;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.shared.exception.BadRequestException;
import ai.planmate.shared.exception.ResourceNotFoundException;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final EntityManager entityManager;

    @Transactional
    public Sprint createSprint(CreateSprintRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        Project project =
                projectRepository
                        .findByIdAndNotDeleted(request.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName(request.getName());
        sprint.setGoal(request.getGoal());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setStatus(SprintStatus.PLANNED);

        return sprintRepository.save(sprint);
    }

    @Transactional
    public void addIssueToSprint(UUID sprintId, UUID issueId) {
        Sprint sprint =
                sprintRepository
                        .findByIdAndNotDeleted(sprintId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        Issue issue =
                issueRepository
                        .findByIdAndNotDeleted(issueId)
                        .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        SprintIssue sprintIssue = new SprintIssue();
        sprintIssue.setSprint(sprint);
        sprintIssue.setIssue(issue);

        entityManager.persist(sprintIssue);
    }

    @Transactional(readOnly = true)
    public List<Sprint> getProjectSprints(UUID projectId) {
        return sprintRepository.findByProjectId(projectId);
    }
}
