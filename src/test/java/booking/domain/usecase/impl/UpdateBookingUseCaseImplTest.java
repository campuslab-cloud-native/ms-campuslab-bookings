package booking.domain.usecase.impl;

import booking.common.exception.InvalidBookingStatusException;
import booking.data.exchange.catalog.CatalogAvailabilityClient;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateBookingUseCaseImplTest {

    private final BookingRepository repository = mock(BookingRepository.class);
    private final CatalogAvailabilityClient catalogAvailabilityClient =
            mock(CatalogAvailabilityClient.class);
    private final UpdateBookingUseCaseImpl useCase =
            new UpdateBookingUseCaseImpl(repository, catalogAvailabilityClient);

    @Test
    void startsPreparationTwentyMinutesBeforeUse() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(20);
        Booking booking = booking(BookingStatus.APPROVED, start, start.plusHours(1));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.advanceAutomatically(1L);

        assertEquals(BookingStatus.IN_PREPARATION, booking.getStatus());
    }

    @Test
    void changesPreparationToReadyTenMinutesBeforeUse() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        Booking booking = booking(
                BookingStatus.IN_PREPARATION,
                start,
                start.plusHours(1)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.advanceAutomatically(1L);

        assertEquals(BookingStatus.READY, booking.getStatus());
    }

    @Test
    void terminatesBookingAndReleasesCapacityAtEndTime() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = booking(
                BookingStatus.IN_USE,
                now.minusHours(1),
                now.minusSeconds(1)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.advanceAutomatically(1L);

        assertEquals(BookingStatus.TERMINATED, booking.getStatus());
        verify(catalogAvailabilityClient).release(7L, "booking:1");
    }

    @Test
    void cancelsReadyBookingAndReleasesCapacity() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        Booking booking = booking(BookingStatus.READY, start, start.plusHours(1));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.execute(1L, BookingStatus.CANCELLED);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(catalogAvailabilityClient).release(7L, "booking:1");
    }

    @Test
    void doesNotAllowCancellingBookingOnceUseHasStarted() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = booking(
                BookingStatus.IN_USE,
                now.minusMinutes(1),
                now.plusHours(1)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertThrows(
                InvalidBookingStatusException.class,
                () -> useCase.execute(1L, BookingStatus.CANCELLED)
        );
        verify(catalogAvailabilityClient, never()).release(7L, "booking:1");
    }

    private Booking booking(
            BookingStatus status,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return Booking.builder()
                .id(1L)
                .resourceId(7L)
                .startTime(start)
                .endTime(end)
                .status(status)
                .build();
    }
}
