package booking.domain.repository;

import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    List<Booking> findByFilters(
            BookingStatus status,
            LocalDate from,
            LocalDate to
    );

    List<Booking> findByUserId(String userId);

    Optional<Booking> findById(Long id);

    Optional<Booking> findByIdForUpdate(Long id);

    List<Booking> findByStatuses(Collection<BookingStatus> statuses);

    Booking save(Booking booking);
}
