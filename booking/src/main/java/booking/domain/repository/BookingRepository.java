package booking.domain.repository;

import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    List<Booking> findByFilters(
            BookingStatus status,
            LocalDate from,
            LocalDate to
    );
    Optional<Booking> findByid(Long id);

    Booking save(Booking booking);
}
