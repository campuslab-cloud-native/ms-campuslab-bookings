package booking.domain.usecase.impl;

import booking.common.exception.BookingNotFoundException;
import booking.common.exception.InvalidBookingStatusException;
import booking.data.exchange.catalog.CatalogAvailabilityClient;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.UpdateBookingStatusUseCase;
import booking.messaging.publisher.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpdateBookingUseCaseImpl implements UpdateBookingStatusUseCase {

    private final BookingRepository repository;
    private final CatalogAvailabilityClient catalogAvailabilityClient;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public Booking execute(Long id, BookingStatus newStatus) {
        Booking booking = repository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new BookingNotFoundException(id)
                );

        validateStatusChange(booking, newStatus);
        booking.setStatus(newStatus);
        Booking savedBooking = repository.save(booking);
        if (newStatus == BookingStatus.APPROVED) {
            notificationPublisher.publishBookingApproved(savedBooking);
        } else if (newStatus == BookingStatus.RETURNED) {
            notificationPublisher.publishBookingReturned(savedBooking);
        }
        if (newStatus == BookingStatus.CANCELLED
                || newStatus == BookingStatus.RETURNED) {
            catalogAvailabilityClient.release(
                    booking.getResourceId(),
                    "booking:" + booking.getId()
            );
        }
        return savedBooking;
    }

    @Override
    @Transactional
    public Booking advanceAutomatically(Long id) {
        Booking booking = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        BookingStatus nextStatus = scheduledStatus(booking, LocalDateTime.now());
        if (nextStatus == booking.getStatus()) {
            return booking;
        }

        booking.setStatus(nextStatus);
        Booking savedBooking = repository.save(booking);
        return savedBooking;
    }

    private void validateStatusChange(Booking booking, BookingStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidBookingStatusException("El estado es obligatorio");
        }

        if (newStatus == BookingStatus.CANCELLED) {
            if (booking.getStatus() == BookingStatus.REQUESTED
                    || booking.getStatus() == BookingStatus.APPROVED
                    || booking.getStatus() == BookingStatus.IN_PREPARATION) {
                if (!LocalDateTime.now().isBefore(booking.getStartTime())) {
                    throw new InvalidBookingStatusException(
                            "La reserva ya alcanzó el horario de inicio y no se puede cancelar"
                    );
                }
                return;
            }
            throw new InvalidBookingStatusException(
                    "La reserva solo se puede cancelar antes de comenzar el uso"
            );
        }

        if (newStatus == BookingStatus.APPROVED
                && booking.getStatus() == BookingStatus.REQUESTED) {
            if (!booking.getStartTime().isAfter(LocalDateTime.now())) {
                throw new InvalidBookingStatusException(
                        "No se puede aprobar una reserva cuyo horario de uso ya comenzó"
                );
            }
            return;
        }

        if (newStatus == BookingStatus.RETURNED
                && booking.getStatus() == BookingStatus.IN_USE) {
            return;
        }

        throw new InvalidBookingStatusException(
                "Transición de estado no permitida: "
                        + booking.getStatus() + " -> " + newStatus
        );
    }

    private BookingStatus scheduledStatus(
            Booking booking,
            LocalDateTime now
    ) {
        return switch (booking.getStatus()) {
            case APPROVED -> now.isBefore(booking.getStartTime().minusMinutes(30))
                    ? BookingStatus.APPROVED
                    : BookingStatus.IN_PREPARATION;
            case IN_PREPARATION -> now.isBefore(booking.getStartTime())
                    ? BookingStatus.IN_PREPARATION
                    : BookingStatus.IN_USE;
            default -> booking.getStatus();
        };
    }
}
