package ai.planmate.chat.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.chat.dto.ChatMessageResponse;
import ai.planmate.chat.dto.ChatThreadResponse;
import ai.planmate.chat.dto.SendMessageRequest;
import ai.planmate.chat.service.ChatService;
import ai.planmate.chat.service.CommandParserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/projects/{projectId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CommandParserService commandParserService;

    @GetMapping("/threads")
    public List<ChatThreadResponse> getThreads(@PathVariable UUID projectId) {
        return chatService.getThreads(projectId);
    }

    @GetMapping("/threads/{threadId}/messages")
    public List<ChatMessageResponse> getMessages(
            @PathVariable UUID projectId, @PathVariable UUID threadId) {
        return chatService.getMessages(threadId);
    }

    @PostMapping("/threads/{threadId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable UUID projectId,
            @PathVariable UUID threadId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AppUser user) {

        ChatMessageResponse userMessage =
                chatService.sendMessage(threadId, user, request.getContent());

        // Process command if present
        commandParserService.parseAndExecute(projectId, threadId, user, request.getContent());

        return userMessage;
    }
}
