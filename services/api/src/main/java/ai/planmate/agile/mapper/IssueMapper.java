package ai.planmate.agile.mapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ai.planmate.agile.dto.IssueResponse;
import ai.planmate.agile.dto.IssueSummaryDto;
import ai.planmate.agile.entity.Issue;

/** Maps Issue entities to frontend DTOs with enum transformations */
public final class IssueMapper {

    private IssueMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Entity → Full IssueResponse (for detail views) */
    public static IssueResponse toResponse(Issue issue) {
        if (issue == null) {
            return null;
        }

        return IssueResponse.builder()
                .id(issue.getId() != null ? issue.getId().toString() : null)
                .projectId(
                        issue.getProject() != null ? issue.getProject().getId().toString() : null)
                .epicId(issue.getEpic() != null ? issue.getEpic().getId().toString() : null)
                .key(issue.getKey())
                .type(EnumMapper.toFrontendType(issue.getType()))
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(EnumMapper.toFrontendStatus(issue.getStatus()))
                .priority(EnumMapper.toFrontendPriority(issue.getPriority()))
                .storyPoints(issue.getStoryPoints())
                .assigneeId(
                        issue.getAssignee() != null ? issue.getAssignee().getId().toString() : null)
                .reporterId(
                        issue.getReporter() != null ? issue.getReporter().getId().toString() : null)
                .labels(
                        issue.getLabels() != null
                                ? Arrays.asList(issue.getLabels())
                                : Collections.emptyList())
                .originalEstimateHours(issue.getOriginalEstimateHours())
                .remainingEstimateHours(issue.getRemainingEstimateHours())
                .timeSpentHours(issue.getTimeSpentHours())
                .orderIndex(issue.getOrderIndex())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    /** Entity → Lightweight IssueSummaryDto (for board views) */
    public static IssueSummaryDto toSummary(Issue issue) {
        if (issue == null) {
            return null;
        }

        return IssueSummaryDto.builder()
                .id(issue.getId() != null ? issue.getId().toString() : null)
                .projectId(
                        issue.getProject() != null ? issue.getProject().getId().toString() : null)
                .key(issue.getKey())
                .type(EnumMapper.toFrontendType(issue.getType()))
                .title(issue.getTitle())
                .status(EnumMapper.toFrontendStatus(issue.getStatus()))
                .priority(EnumMapper.toFrontendPriority(issue.getPriority()))
                .storyPoints(issue.getStoryPoints())
                .assigneeId(
                        issue.getAssignee() != null ? issue.getAssignee().getId().toString() : null)
                .labels(
                        issue.getLabels() != null
                                ? Arrays.asList(issue.getLabels())
                                : Collections.emptyList())
                .orderIndex(issue.getOrderIndex())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    /** List of entities → List of responses */
    public static List<IssueResponse> toResponseList(List<Issue> issues) {
        if (issues == null) {
            return Collections.emptyList();
        }
        return issues.stream().map(IssueMapper::toResponse).toList();
    }

    /** List of entities → List of summaries (for board) */
    public static List<IssueSummaryDto> toSummaryList(List<Issue> issues) {
        if (issues == null) {
            return Collections.emptyList();
        }
        return issues.stream().map(IssueMapper::toSummary).toList();
    }
}
