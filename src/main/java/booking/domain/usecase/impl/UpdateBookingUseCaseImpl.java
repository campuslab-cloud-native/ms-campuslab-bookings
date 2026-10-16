package booking.domain.usecase.impl;

import booking.common.exception.BookingNotFoundException;
import booking.common.exception.InvalidBookingStatusException;
import booking.data.exchange.catalog.CatalogAvailabilityClient;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.UpdateBookingStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpdateBookingUseCaseImpl implements UpdateBookingStatusUseCase {

    private final BookingRepository repository;
    private final CatalogAvailabilityClient catalogAvailabilityClient;

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
        if (newStatus == BookingStatus.CANCELLED) {
            catalogAvailabilityClient.release(booking.getResourceId());
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
        if (nextStatus == BookingStatus.TERMINATED) {
            catalogAvailabilityClient.release(booking.getResourceId());
        }
        return savedBooking;
    }

    private void validateStatusChange(Booking booking, BookingStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidBookingStatusException("El estado es obligatorio");
        }

        if (newStatus == BookingStatus.CANCELLED) {
            if (booking.getStatus() == BookingStatus.REQUESTED
                    || booking.getStatus() == BookingStatus.APPROVED
                    || booking.getStatus() == BookingStatus.IN_PREPARATION
                    || booking.getStatus() == BookingStatus.READY) {
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

        throw new InvalidBookingStatusException(
                "Transición de estado no permitida: "
                        + booking.getStatus() + " -> " + newStatus
        );
    }

    private BookingStatus scheduledStatus(
            Booking booking,
            LocalDateTime now
    ) {
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.TERMINATED
                || booking.getStatus() == BookingStatus.RETURNED
                || booking.getStatus() == BookingStatus.REQUESTED) {
            return booking.getStatus();
        }

        LocalDateTime start = booking.getStartTime();
        if (now.isBefore(start.minusMinutes(30))) {
            return BookingStatus.APPROVED;
        }
        if (now.isBefore(start.minusMinutes(10))) {
            return BookingStatus.IN_PREPARATION;
        }
        if (now.isBefore(start)) {
            return BookingStatus.READY;
        }
        if (now.isBefore(booking.getEndTime())) {
            return BookingStatus.IN_USE;
        }
        return BookingStatus.TERMINATED;
    }
}
