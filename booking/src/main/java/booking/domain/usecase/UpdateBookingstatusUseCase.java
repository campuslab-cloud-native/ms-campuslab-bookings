package booking.domain.usecase;

import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;

public interface UpdateBookingstatusUseCase {

    Booking execute(Long id, BookingStatus status);
}
