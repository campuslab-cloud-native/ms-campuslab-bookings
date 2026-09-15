package booking.domain.usecase.impl;

import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.GetBookingsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBookingsUseCaseImpl implements GetBookingsUseCase {

    private final BookingRepository repository;

    @Override
    public List<Booking> execute(
                BookingStatus status,
                LocalDate from,
                LocalDate to) {

        return repository.findByFilters(status,from,to);
    }
}
