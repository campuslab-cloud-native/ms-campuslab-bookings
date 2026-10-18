package booking.messaging.publisher;

import booking.config.MessagingNames;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.messaging.model.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class NotificationPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final NotificationPublisher publisher = new NotificationPublisher(rabbitTemplate);
    private final Booking booking = Booking.builder()
            .id(12L)
            .resourceId(7L)
            .userId("student@example.com")
            .status(BookingStatus.REQUESTED)
            .startTime(LocalDateTime.parse("2026-10-18T10:00:00"))
            .endTime(LocalDateTime.parse("2026-10-18T11:00:00"))
            .build();

    @Test
    void publishesRequestedBookingToEmailCommand() {
        verifyPublished(
                MessagingNames.EMAIL_ROUTING_KEY,
                "BOOKING_REQUESTED",
                publisher::publishBookingRequested
        );
    }

    @Test
    void publishesApprovedBookingToPreparationCommand() {
        booking.setStatus(BookingStatus.APPROVED);
        verifyPublished(
                MessagingNames.PREP_ROUTING_KEY,
                "BOOKING_APPROVED",
                publisher::publishBookingApproved
        );
    }

    @Test
    void publishesReturnedBookingToVoucherCommand() {
        booking.setStatus(BookingStatus.RETURNED);
        verifyPublished(
                MessagingNames.VOUCHER_ROUTING_KEY,
                "BOOKING_RETURNED",
                publisher::publishBookingReturned
        );
    }

    @Test
    void brokerFailureDoesNotEscapePublisher() {
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(MessagingNames.EXCHANGE),
                        eq(MessagingNames.EMAIL_ROUTING_KEY),
                        any(NotificationMessage.class)
                );

        assertDoesNotThrow(() -> publisher.publishBookingRequested(booking));
    }

    private void verifyPublished(
            String routingKey,
            String expectedType,
            java.util.function.Consumer<Booking> publish
    ) {
        publish.accept(booking);

        org.mockito.ArgumentCaptor<NotificationMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(NotificationMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(MessagingNames.EXCHANGE),
                eq(routingKey),
                messageCaptor.capture()
        );
        NotificationMessage message = messageCaptor.getValue();
        assertEquals(expectedType, message.getType());
        assertNotNull(message.getEventId());
        assertNotNull(message.getTimestamp());
        assertNotNull(message.getTraceId());
        assertEquals("12", message.getCorrelationId());
        assertEquals(
                Map.of(
                        "bookingId", 12L,
                        "resourceId", 7L,
                        "userId", "student@example.com",
                        "status", booking.getStatus().name(),
                        "startTime", booking.getStartTime().toString(),
                        "endTime", booking.getEndTime().toString()
                ),
                message.getPayload()
        );
    }
}
