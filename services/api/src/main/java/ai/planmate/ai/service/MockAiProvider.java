package ai.planmate.ai.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean supportsTools() {
        return true;
    }

    @Override
    public AiResult invoke(String prompt, Map<String, Object> tools, Map<String, Object> context) {
        String operation = context != null ? (String) context.getOrDefault("operation", "") : "";

        Map<String, Object> toolOutput = new HashMap<>();
        String content;

        switch (operation) {
            case "EPIC_BREAKDOWN" -> {
                content = "Here's a suggested epic breakdown based on the project context.";
                toolOutput.put(
                        "epics",
                        new String[] {"User Authentication", "Dashboard", "Settings", "Reporting"});
                toolOutput.put("confidence", "HIGH");
            }
            case "DOC_GENERATION" -> {
                content =
                        "# Generated Document\n\n"
                                + "## Overview\n"
                                + "This document was generated based on the project context.\n\n"
                                + "## Requirements\n"
                                + "- Requirement 1\n"
                                + "- Requirement 2\n"
                                + "- Requirement 3";
                toolOutput.put("format", "markdown");
            }
            case "DIAGRAM_GENERATION" -> {
                String diagramType =
                        context != null
                                ? (String) context.getOrDefault("diagramType", "flowchart")
                                : "flowchart";
                content = generateMockDiagram(diagramType);
                toolOutput.put("format", "mermaid");
            }
            default -> {
                content = "AI processing completed successfully. This is a mock response.";
                toolOutput.put("message", "Mock AI provider response");
            }
        }

        return AiResult.success(content, toolOutput, 150);
    }

    private String generateMockDiagram(String type) {
        return switch (type) {
            case "er" -> """
                    erDiagram
                        USER ||--o{ PROJECT : owns
                        PROJECT ||--o{ ISSUE : contains
                        PROJECT ||--o{ SPRINT : has
                        ISSUE }o--o| EPIC : "belongs to"
                        SPRINT ||--o{ ISSUE : includes
                    """;
            case "sequence" -> """
                    sequenceDiagram
                        User->>+API: Create Issue
                        API->>+DB: Save Issue
                        DB-->>-API: Issue Saved
                        API->>+WebSocket: Broadcast Event
                        API-->>-User: Issue Created
                    """;
            case "use_case" -> """
                    flowchart LR
                        User((User))
                        User --> CreateProject[Create Project]
                        User --> ManageBoard[Manage Board]
                        User --> CreateIssue[Create Issue]
                        User --> ChatAI[Chat with AI]
                        ManageBoard --> DragDrop[Drag & Drop]
                        ManageBoard --> AddColumn[Add Column]
                    """;
            default -> """
                    flowchart TD
                        A[Start] --> B{Decision}
                        B -->|Yes| C[Action 1]
                        B -->|No| D[Action 2]
                        C --> E[End]
                        D --> E
                    """;
        };
    }
}
