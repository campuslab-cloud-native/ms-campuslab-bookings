package booking.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    private String type;
    private String eventId;
    private OffsetDateTime timestamp;
    private String traceId;
    private String correlationId;
    private Map<String, Object> payload;
}