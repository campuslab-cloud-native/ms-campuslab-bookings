package booking.domain.usecase.impl;

import booking.common.exception.BookingNotFoundException;
import booking.common.exception.InvalidBookingStatusException;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.UpdateBookingStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateBookingUseCaseImpl implements UpdateBookingStatusUseCase {

    private final BookingRepository repository;
    @Override
    public Booking execute(Long id, BookingStatus newStatus) {
        Booking booking = repository.findById(id)
                .orElseThrow(() ->
                new BookingNotFoundException(id)
                );

        validateStatusChange(booking, newStatus);

        booking.setStatus(newStatus);

        return repository.save(booking)
        ;
    }

    private void validateStatusChange(Booking booking, BookingStatus newStatus) {

        if (newStatus == BookingStatus.IN_USE
                && booking.getStatus() == BookingStatus.REQUESTED) {

            throw new InvalidBookingStatusException(
                    "Booking must be approved before entering IN_USE"
            );
        }
    }
}
