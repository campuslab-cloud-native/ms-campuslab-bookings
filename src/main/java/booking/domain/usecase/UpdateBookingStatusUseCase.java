package booking.domain.usecase;

import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;

public interface UpdateBookingStatusUseCase {

    Booking execute(Long id, BookingStatus status);

    Booking forceUpdateStatus(Long id, BookingStatus status);

    Booking advanceAutomatically(Long id);
}
