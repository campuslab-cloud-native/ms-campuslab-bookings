package booking.domain.usecase.impl;

import booking.common.exception.BookingConflictException;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.CreateBookingUseCase;
import booking.messaging.model.NotificationMessage;
import booking.messaging.publisher.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateBookingUseCaseImpl implements CreateBookingUseCase {

    private final BookingRepository repository;
    private final NotificationPublisher notificationPublisher;

    @Override
    public Booking execute(Booking booking) {

        boolean hasConflict = repository.existsConflict(
                booking.getResourceId(),
                booking.getStartTime(),
                booking.getEndTime()
        );

        if (hasConflict) {
            throw new BookingConflictException(
                    "El laboratorio ya se encuentra reservado en ese horario"
            );
        }

        booking.setStatus(BookingStatus.REQUESTED);
        booking.setCreatedAt(LocalDateTime.now());

        Booking savedBooking = repository.save(booking);

        NotificationMessage notification = NotificationMessage.builder()
        .type("BOOKING_CREATED")
        .eventId(UUID.randomUUID().toString())
        .timestamp(OffsetDateTime.now())
        .traceId(UUID.randomUUID().toString())
        .correlationId(savedBooking.getId().toString())
        .payload(Map.of(
                "bookingId", savedBooking.getId(),
                "resourceId", savedBooking.getResourceId(),
                "userId", savedBooking.getUserId(),
                "startTime", savedBooking.getStartTime().toString(),
                "endTime", savedBooking.getEndTime().toString()
        ))
        .build();

        notificationPublisher.publishEmail(notification);

        return savedBooking;
    }
}
