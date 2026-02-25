package ai.planmate.agile.mapper;

import ai.planmate.agile.entity.IssuePriority;
import ai.planmate.agile.entity.IssueStatus;
import ai.planmate.agile.entity.IssueType;
import ai.planmate.agile.entity.SprintStatus;

/**
 * Maps backend enums to frontend format and vice versa Backend uses UPPERCASE (SELECTED, STORY,
 * CRITICAL, PLANNED) Frontend uses lowercase with dashes (todo, feature, urgent, planning)
 */
public final class EnumMapper {

    private EnumMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== IssueStatus Mapping ====================

    /** Backend SELECTED → Frontend "todo" Others remain same but lowercase */
    public static String toFrontendStatus(IssueStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case BACKLOG -> "backlog";
            case SELECTED -> "todo"; // Key mapping!
            case IN_PROGRESS -> "in-progress";
            case REVIEW -> "review";
            case DONE -> "done";
        };
    }

    /** Frontend "todo" → Backend SELECTED */
    public static IssueStatus toBackendStatus(String frontendStatus) {
        if (frontendStatus == null) {
            return null;
        }

        return switch (frontendStatus.toLowerCase()) {
            case "backlog" -> IssueStatus.BACKLOG;
            case "todo" -> IssueStatus.SELECTED; // Key mapping!
            case "in-progress" -> IssueStatus.IN_PROGRESS;
            case "review" -> IssueStatus.REVIEW;
            case "done" -> IssueStatus.DONE;
            default -> throw new IllegalArgumentException("Unknown status: " + frontendStatus);
        };
    }

    // ==================== IssueType Mapping ====================

    /**
     * Backend STORY → Frontend "feature" Backend BUG → Frontend "bug" Backend TASK → Frontend can
     * be "task" or "improvement"
     */
    public static String toFrontendType(IssueType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case STORY -> "feature"; // Key mapping!
            case TASK -> "task";
            case BUG -> "bug";
        };
    }

    /** Frontend "feature" or "improvement" → Backend STORY or TASK */
    public static IssueType toBackendType(String frontendType) {
        if (frontendType == null) {
            return null;
        }

        return switch (frontendType.toLowerCase()) {
            case "feature" -> IssueType.STORY; // Key mapping!
            case "improvement" -> IssueType.TASK; // improvement treated as TASK
            case "task" -> IssueType.TASK;
            case "bug" -> IssueType.BUG;
            default -> throw new IllegalArgumentException("Unknown type: " + frontendType);
        };
    }

    // ==================== IssuePriority Mapping ====================

    /** Backend CRITICAL → Frontend "urgent" Others remain same but lowercase */
    public static String toFrontendPriority(IssuePriority priority) {
        if (priority == null) {
            return null;
        }

        return switch (priority) {
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
            case CRITICAL -> "urgent"; // Key mapping!
        };
    }

    /** Frontend "urgent" → Backend CRITICAL */
    public static IssuePriority toBackendPriority(String frontendPriority) {
        if (frontendPriority == null) {
            return null;
        }

        return switch (frontendPriority.toLowerCase()) {
            case "low" -> IssuePriority.LOW;
            case "medium" -> IssuePriority.MEDIUM;
            case "high" -> IssuePriority.HIGH;
            case "urgent" -> IssuePriority.CRITICAL; // Key mapping!
            default -> throw new IllegalArgumentException("Unknown priority: " + frontendPriority);
        };
    }

    // ==================== SprintStatus Mapping ====================

    /** Backend PLANNED → Frontend "planning" Others remain same but lowercase */
    public static String toFrontendSprintStatus(SprintStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case PLANNED -> "planning"; // Key mapping!
            case ACTIVE -> "active";
            case COMPLETED -> "completed";
        };
    }

    /** Frontend "planning" → Backend PLANNED */
    public static SprintStatus toBackendSprintStatus(String frontendStatus) {
        if (frontendStatus == null) {
            return null;
        }

        return switch (frontendStatus.toLowerCase()) {
            case "planning" -> SprintStatus.PLANNED; // Key mapping!
            case "active" -> SprintStatus.ACTIVE;
            case "completed" -> SprintStatus.COMPLETED;
            default -> throw new IllegalArgumentException(
                    "Unknown sprint status: " + frontendStatus);
        };
    }
}
