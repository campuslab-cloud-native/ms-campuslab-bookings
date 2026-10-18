package booking.domain.usecase.impl;

import booking.common.exception.BookingRuleViolationException;
import booking.data.exchange.catalog.CatalogAvailabilityClient;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.CreateBookingUseCase;
import booking.messaging.publisher.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateBookingUseCaseImpl implements CreateBookingUseCase {

    private final BookingRepository repository;
    private final NotificationPublisher notificationPublisher;
    private final CatalogAvailabilityClient catalogAvailabilityClient;

    @Override
    @Transactional
    public Booking execute(Booking booking) {
        validateBookingTime(booking);
        catalogAvailabilityClient.reserve(booking.getResourceId());

        booking.setStatus(BookingStatus.REQUESTED);
        booking.setCreatedAt(LocalDateTime.now());

        try {
            Booking savedBooking = repository.save(booking);
            notificationPublisher.publishBookingRequested(savedBooking);
            return savedBooking;
        } catch (RuntimeException exception) {
            try {
                catalogAvailabilityClient.release(
                        booking.getResourceId(),
                        "booking-compensation:" + UUID.randomUUID()
                );
            } catch (RuntimeException compensationException) {
                exception.addSuppressed(compensationException);
                log.error(
                        "Failed to restore resource availability after booking creation failed. resourceId={}",
                        booking.getResourceId(),
                        compensationException
                );
            }
            throw exception;
        }
    }

    private void validateBookingTime(Booking booking) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        if (booking.getStartTime().toLocalDate().isBefore(today)) {
            throw new BookingRuleViolationException(
                    "La fecha de inicio no puede ser anterior al día actual"
            );
        }
        if (!booking.getStartTime().isAfter(now)) {
            throw new BookingRuleViolationException(
                    "La hora de inicio debe ser posterior a la hora actual"
            );
        }
        if (!booking.getEndTime().isAfter(booking.getStartTime())) {
            throw new BookingRuleViolationException(
                    "La hora de término debe ser posterior a la hora de inicio"
            );
        }
        if (Duration.between(booking.getStartTime(), booking.getEndTime())
                .compareTo(Duration.ofHours(2)) > 0) {
            throw new BookingRuleViolationException(
                    "La reserva no puede durar más de 2 horas"
            );
        }
    }

}
