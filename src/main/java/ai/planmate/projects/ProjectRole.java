package ai.planmate.projects;

/**
 * Project-level roles for access control within a specific project. These roles are scoped to
 * individual projects, independent of workspace roles.
 */
public enum ProjectRole {
    ADMIN, // Full project control, can manage members and settings
    MEMBER, // Can create and edit issues, manage sprints
    VIEWER // Read-only access to project
}
