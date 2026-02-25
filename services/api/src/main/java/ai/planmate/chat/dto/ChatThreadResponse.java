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
public class ChatThreadResponse {
    private UUID id;
    private UUID projectId;
    private String title;
    private Boolean isDefault;
    private Instant createdAt;
}
