package ai.planmate.realtime;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastBoardUpdate(UUID projectId, RealtimeEvent event) {
        String destination = "/topic/projects/" + projectId + "/board";
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Broadcast board event to { }: { }", destination, event.getType());
    }

    public void broadcastChatMessage(UUID threadId, RealtimeEvent event) {
        String destination = "/topic/threads/" + threadId;
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Broadcast chat event to { }: { }", destination, event.getType());
    }

    public void broadcastProjectEvent(UUID projectId, RealtimeEvent event) {
        String destination = "/topic/projects/" + projectId;
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Broadcast project event to { }: { }", destination, event.getType());
    }
}
