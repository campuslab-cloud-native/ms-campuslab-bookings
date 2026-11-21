package booking.messaging.model;

import java.time.OffsetDateTime;

public record BookingEvent(
        String eventId,
        String type,
        Long bookingId,
        Long resourceId,
        String userId,
        OffsetDateTime timestamp,
        String traceId,
        String correlationId
) {
}

