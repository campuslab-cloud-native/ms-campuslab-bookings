package booking.domain.repository;

import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    List<Booking> findByFilters(
            BookingStatus status,
            LocalDate from,
            LocalDate to
    );

    Optional<Booking> findById(Long id);

    boolean existsConflict(
            Long resourceId,
            LocalDateTime start,
            LocalDateTime end
    );

    Booking save(Booking booking);
}
