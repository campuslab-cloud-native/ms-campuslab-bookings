package booking.domain.usecase;

import booking.domain.model.Booking;

public interface CreateBookingUseCase {

    Booking execute(Booking booking);
}
