package booking.domain.usecase.impl;

import booking.common.exception.BookingNotFoundException;
import booking.domain.model.Booking;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.GetBookingByIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBookingByIdUseCaseImpl implements GetBookingByIdUseCase {

    private final BookingRepository repository;

    @Override
    public Booking execute(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new BookingNotFoundException(id)
                );
    }
}
