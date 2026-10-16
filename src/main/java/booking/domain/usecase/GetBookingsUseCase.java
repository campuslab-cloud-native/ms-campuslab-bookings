package booking.domain.usecase;

import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;

import java.time.LocalDate;
import java.util.List;

public interface GetBookingsUseCase {

    List<Booking> execute(
            BookingStatus status,
            LocalDate from,
            LocalDate to
    );

    List<Booking> executeByUserId(String userId);
}
