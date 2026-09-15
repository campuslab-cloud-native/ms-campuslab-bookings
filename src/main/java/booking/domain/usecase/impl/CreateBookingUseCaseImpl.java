package booking.domain.usecase.impl;

import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.CreateBookingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateBookingUseCaseImpl implements CreateBookingUseCase {

    private final BookingRepository repository;

    @Override
    public Booking execute(Booking booking) {

        booking.setStatus(BookingStatus.REQUESTED);
        booking.setCreatedAt(LocalDateTime.now());

        return repository.save(booking);
    }
}
