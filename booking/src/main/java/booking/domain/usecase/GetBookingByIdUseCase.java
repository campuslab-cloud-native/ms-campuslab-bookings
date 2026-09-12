package booking.domain.usecase;

import booking.domain.model.Booking;

public interface GetBookingByIdUseCase {

    Booking execute(Long id);
}
