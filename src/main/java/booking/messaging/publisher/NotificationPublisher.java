package booking.messaging.publisher;

import booking.config.MessagingNames;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.messaging.model.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishBookingRequested(Booking booking) {
        publish(
                MessagingNames.EMAIL_ROUTING_KEY,
                createMessage(booking, "BOOKING_REQUESTED")
        );
    }

    public void publishBookingApproved(Booking booking) {
        publish(
                MessagingNames.PREP_ROUTING_KEY,
                createMessage(booking, "BOOKING_APPROVED")
        );
    }

    public void publishBookingReturned(Booking booking) {
        publish(
                MessagingNames.VOUCHER_ROUTING_KEY,
                createMessage(booking, "BOOKING_RETURNED")
        );
    }

    private void publish(String routingKey, NotificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    MessagingNames.EXCHANGE,
                    routingKey,
                    message
            );

            log.info(
                    "Notification command published. eventId={}, type={}, routingKey={}",
                    message.getEventId(),
                    message.getType(),
                    routingKey
            );
        } catch (AmqpException exception) {
            log.error(
                    "Failed to publish notification command; booking persistence will continue. eventId={}, routingKey={}",
                    message.getEventId(),
                    routingKey,
                    exception
            );
        }
    }

    private NotificationMessage createMessage(Booking booking, String type) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("resourceId", booking.getResourceId());
        payload.put("status", booking.getStatus().name());
        payload.put("startTime", booking.getStartTime().toString());
        payload.put("endTime", booking.getEndTime().toString());
        if (booking.getUserId() != null) {
            payload.put("userId", booking.getUserId());
        }

        return NotificationMessage.builder()
                .type(type)
                .eventId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now())
                .traceId(UUID.randomUUID().toString())
                .correlationId(booking.getId().toString())
                .payload(payload)
                .build();
    }
}