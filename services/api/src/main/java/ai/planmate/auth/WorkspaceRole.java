package ai.planmate.auth;

/** Workspace-level roles for fine-grained access control. */
public enum WorkspaceRole {
    OWNER, // Full control, can delete workspace
    MANAGER, // Can manage members and projects
    EDITOR, // Can edit issues and artifacts
    VIEWER, // Read-only access
    COMMENTER // Can view and comment
}
