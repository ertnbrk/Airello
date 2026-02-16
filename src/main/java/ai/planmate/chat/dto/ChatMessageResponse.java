package ai.planmate.chat.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private UUID id;
    private UUID threadId;
    private UUID senderId;
    private String senderType;
    private String content;
    private Object toolCalls;
    private Instant createdAt;
}
