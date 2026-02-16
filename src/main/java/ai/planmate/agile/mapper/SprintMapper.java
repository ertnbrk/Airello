package ai.planmate.agile.mapper;

import java.util.Collections;
import java.util.List;

import ai.planmate.agile.dto.SprintResponse;
import ai.planmate.agile.entity.Sprint;

/** Maps Sprint entities to frontend DTOs with enum transformations */
public final class SprintMapper {

    private SprintMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Entity → SprintResponse */
    public static SprintResponse toResponse(Sprint sprint) {
        if (sprint == null) {
            return null;
        }

        return SprintResponse.builder()
                .id(sprint.getId() != null ? sprint.getId().toString() : null)
                .projectId(
                        sprint.getProject() != null ? sprint.getProject().getId().toString() : null)
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(EnumMapper.toFrontendSprintStatus(sprint.getStatus()))
                .createdAt(sprint.getCreatedAt())
                .updatedAt(sprint.getUpdatedAt())
                .build();
    }

    /** List of entities → List of responses */
    public static List<SprintResponse> toResponseList(List<Sprint> sprints) {
        if (sprints == null) {
            return Collections.emptyList();
        }
        return sprints.stream().map(SprintMapper::toResponse).toList();
    }
}
