package ai.planmate.chat.service;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.planmate.agile.entity.Epic;
import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.entity.IssueType;
import ai.planmate.agile.repository.EpicRepository;
import ai.planmate.agile.repository.IssueRepository;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.diagram.dto.GenerateDiagramRequest;
import ai.planmate.diagram.service.DiagramService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandParserService {

    private final ChatService chatService;
    private final EpicRepository epicRepository;
    private final IssueRepository issueRepository;
    private final DiagramService diagramService;

    private static final Pattern CREATE_TASK =
            Pattern.compile("^/create\\s+task\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_EPIC =
            Pattern.compile("^/create\\s+epic\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOVE_ISSUE =
            Pattern.compile("^/move\\s+([A-Z]+-\\d+)\\s+to\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LABEL_ISSUE =
            Pattern.compile("^/label\\s+([A-Z]+-\\d+)\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERATE_DIAGRAM =
            Pattern.compile("^/generate\\s+diagram\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    public void parseAndExecute(UUID projectId, UUID threadId, AppUser user, String content) {
        if (!content.startsWith("/")) {
            return; // Not a command
        }

        try {
            Matcher m;

            m = CREATE_TASK.matcher(content);
            if (m.matches()) {
                executeCreateTask(projectId, threadId, user, m.group(1));
                return;
            }

            m = CREATE_EPIC.matcher(content);
            if (m.matches()) {
                executeCreateEpic(projectId, threadId, user, m.group(1));
                return;
            }

            m = MOVE_ISSUE.matcher(content);
            if (m.matches()) {
                executeMoveIssue(projectId, threadId, m.group(1), m.group(2));
                return;
            }

            m = LABEL_ISSUE.matcher(content);
            if (m.matches()) {
                executeLabelIssue(projectId, threadId, m.group(1), m.group(2));
                return;
            }

            m = GENERATE_DIAGRAM.matcher(content);
            if (m.matches()) {
                executeGenerateDiagram(projectId, threadId, user, m.group(1));
                return;
            }

            chatService.addSystemMessage(
                    threadId,
                    "Unknown command. Try `/create task <title>`, `/create epic <title>`, "
                            + "`/move <issue-key> to <column>`, `/label <issue-key> <label>`, "
                            + "or `/generate diagram <type>`.");
        } catch (Exception e) {
            log.error("Command execution error", e);
            chatService.addSystemMessage(threadId, "Error executing command: " + e.getMessage());
        }
    }

    private void executeCreateTask(UUID projectId, UUID threadId, AppUser user, String title) {
        Issue issue = new Issue();
        issue.setProject(new ai.planmate.projects.entity.Project());
        issue.getProject().setId(projectId);
        issue.setKey("AUTO-" + System.currentTimeMillis() % 10000);
        issue.setTitle(title);
        issue.setType(IssueType.TASK);
        issue.setReporter(user);
        issue = issueRepository.save(issue);

        chatService.addSystemMessage(
                threadId, "✓ Created task **" + issue.getKey() + "**: " + title);
    }

    private void executeCreateEpic(UUID projectId, UUID threadId, AppUser user, String name) {
        Epic epic = new Epic();
        epic.setProject(new ai.planmate.projects.entity.Project());
        epic.getProject().setId(projectId);
        epic.setTitle(name);
        epic = epicRepository.save(epic);

        chatService.addSystemMessage(threadId, "✓ Created epic: **" + name + "**");
    }

    private void executeMoveIssue(
            UUID projectId, UUID threadId, String issueKey, String columnName) {
        chatService.addSystemMessage(
                threadId,
                "Move command recognized: "
                        + issueKey
                        + " → "
                        + columnName
                        + " (full implementation pending)");
    }

    private void executeLabelIssue(UUID projectId, UUID threadId, String issueKey, String label) {
        chatService.addSystemMessage(
                threadId,
                "Label command recognized: "
                        + issueKey
                        + " + "
                        + label
                        + " (full implementation pending)");
    }

    private void executeGenerateDiagram(
            UUID projectId, UUID threadId, AppUser user, String diagramType) {
        GenerateDiagramRequest request = new GenerateDiagramRequest();
        request.setType(diagramType);
        request.setFormat("mermaid");

        try {
            var diagram = diagramService.generateDiagram(projectId, user, request);
            chatService.addSystemMessage(
                    threadId,
                    "✓ Generated "
                            + diagramType
                            + " diagram:\n```mermaid\n"
                            + diagram.getContent()
                            + "\n```");
        } catch (Exception e) {
            chatService.addSystemMessage(threadId, "Error generating diagram: " + e.getMessage());
        }
    }
}
