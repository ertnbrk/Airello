package ai.planmate.realtime;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEvent {
    private String type;
    private Object payload;
    private Instant timestamp;

    public static RealtimeEvent of(String type, Object payload) {
        return RealtimeEvent.builder().type(type).payload(payload).timestamp(Instant.now()).build();
    }
}
