package ai.planmate.chat.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.chat.dto.ChatMessageResponse;
import ai.planmate.chat.dto.ChatThreadResponse;
import ai.planmate.chat.entity.ChatMessage;
import ai.planmate.chat.entity.ChatThread;
import ai.planmate.chat.entity.SenderType;
import ai.planmate.chat.repository.ChatMessageRepository;
import ai.planmate.chat.repository.ChatThreadRepository;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.realtime.RealtimeEvent;
import ai.planmate.realtime.RealtimeEventService;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final RealtimeEventService realtimeEventService;

    @Transactional
    public ChatThread createDefaultThread(Project project, AppUser creator) {
        ChatThread thread = new ChatThread();
        thread.setProject(project);
        thread.setCreatedBy(creator);
        thread.setTitle("Project Chat");
        thread.setIsDefault(true);
        thread = threadRepository.save(thread);

        ChatMessage welcome = new ChatMessage();
        welcome.setThread(thread);
        welcome.setSenderType(SenderType.SYSTEM);
        welcome.setContent(
                "Welcome to your project! Try these commands:\n"
                        + "- `/create task <title>` - Create a new task\n"
                        + "- `/create epic <title>` - Create a new epic\n"
                        + "- `/move <issue-key> to <column>` - Move an issue\n"
                        + "- `/label <issue-key> <label>` - Add a label\n"
                        + "- `/generate diagram <type>` - Generate a diagram\n"
                        + "- Or just ask me anything about your project!");
        messageRepository.save(welcome);

        return thread;
    }

    @Transactional(readOnly = true)
    public List<ChatThreadResponse> getThreads(UUID projectId) {
        return threadRepository.findByProjectId(projectId).stream()
                .map(this::toThreadResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID threadId) {
        return messageRepository.findByThreadIdOrderByCreatedAt(threadId).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID threadId, AppUser sender, String content) {
        ChatThread thread =
                threadRepository
                        .findById(threadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));

        ChatMessage message = new ChatMessage();
        message.setThread(thread);
        message.setSender(sender);
        message.setSenderType(SenderType.USER);
        message.setContent(content);
        message = messageRepository.save(message);

        ChatMessageResponse response = toMessageResponse(message);

        realtimeEventService.broadcastChatMessage(
                threadId, RealtimeEvent.of("CHAT_MESSAGE_CREATED", response));

        return response;
    }

    @Transactional
    public ChatMessage addSystemMessage(UUID threadId, String content) {
        ChatThread thread =
                threadRepository
                        .findById(threadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));

        ChatMessage message = new ChatMessage();
        message.setThread(thread);
        message.setSenderType(SenderType.SYSTEM);
        message.setContent(content);
        message = messageRepository.save(message);

        realtimeEventService.broadcastChatMessage(
                threadId, RealtimeEvent.of("CHAT_MESSAGE_CREATED", toMessageResponse(message)));

        return message;
    }

    @Transactional
    public ChatMessage addAiMessage(UUID threadId, String content, Object toolCalls) {
        ChatThread thread =
                threadRepository
                        .findById(threadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));

        ChatMessage message = new ChatMessage();
        message.setThread(thread);
        message.setSenderType(SenderType.AI);
        message.setContent(content);
        message.setToolCalls(toolCalls);
        message = messageRepository.save(message);

        realtimeEventService.broadcastChatMessage(
                threadId, RealtimeEvent.of("CHAT_MESSAGE_CREATED", toMessageResponse(message)));

        return message;
    }

    public ChatThread getDefaultThread(UUID projectId) {
        return threadRepository
                .findDefaultByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Default thread not found"));
    }

    private ChatThreadResponse toThreadResponse(ChatThread thread) {
        return ChatThreadResponse.builder()
                .id(thread.getId())
                .projectId(thread.getProject().getId())
                .title(thread.getTitle())
                .isDefault(thread.getIsDefault())
                .createdAt(thread.getCreatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .threadId(message.getThread().getId())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderType(message.getSenderType().name())
                .content(message.getContent())
                .toolCalls(message.getToolCalls())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
