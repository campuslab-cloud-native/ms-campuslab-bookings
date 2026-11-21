package booking.domain.port;

import booking.domain.model.Booking;

public interface BookingEventPublisherPort {

    void publishBookingCreated(Booking booking);

    void publishBookingStatusChanged(Booking booking);

}
