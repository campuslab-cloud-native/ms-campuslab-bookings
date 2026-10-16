package booking.domain.usecase.impl;

import booking.domain.model.Booking;
import booking.domain.repository.BookingRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetBookingsUseCaseImplTest {

    private final BookingRepository repository = mock(BookingRepository.class);
    private final GetBookingsUseCaseImpl useCase =
            new GetBookingsUseCaseImpl(repository);

    @Test
    void returnsOnlyBookingsForRequestedUser() {
        String userId = "student-123";
        List<Booking> bookings = List.of(
                Booking.builder().id(5L).userId(userId).build()
        );
        when(repository.findByUserId(userId)).thenReturn(bookings);

        assertEquals(bookings, useCase.executeByUserId(userId));
        verify(repository).findByUserId(userId);
    }
}
