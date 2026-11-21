
package booking.messaging.kafka;

import booking.domain.model.Booking;
import booking.domain.port.BookingEventPublisherPort;
import booking.messaging.model.BookingEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBookingPublisherAdapter
        implements BookingEventPublisherPort {

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @Value("${campuslab.kafka.topics.bookings}")
    private String bookingsTopic;

    @Override
    public void publishBookingCreated(Booking booking) {
        publish(booking, "BOOKING_CREATED");
    }

    @Override
    public void publishBookingStatusChanged(Booking booking) {
        String eventType = switch (booking.getStatus()) {
            case APPROVED -> "BOOKING_APPROVED";
            case IN_PREPARATION -> "BOOKING_PREPARING";
            case IN_USE -> "BOOKING_IN_USE";
            case RETURNED -> "BOOKING_RETURNED";
            case CANCELLED -> "BOOKING_CANCELLED";
            case REQUESTED -> throw new IllegalArgumentException(
                    "REQUESTED corresponde a la creacion"
            );
        };

        publish(booking, eventType);
    }

    private void publish(Booking booking, String eventType) {

        BookingEvent event = new BookingEvent(
                UUID.randomUUID().toString(),
                eventType,
                booking.getId(),
                booking.getResourceId(),
                booking.getUserId(),
                OffsetDateTime.now(ZoneOffset.UTC),
                UUID.randomUUID().toString(),
                booking.getId().toString()
        );

        String key = booking.getId().toString();

        kafkaTemplate.send(bookingsTopic, key, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Error publicando evento Kafka. bookingId={}, type={}",
                                booking.getId(),
                                eventType,
                                exception
                        );
                        return;
                    }

                    log.info(
                            "Evento Kafka publicado. bookingId={}, type={}, partition={}, offset={}",
                            booking.getId(),
                            eventType,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
