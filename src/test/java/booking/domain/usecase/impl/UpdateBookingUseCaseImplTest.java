package booking.domain.usecase.impl;

import booking.common.exception.InvalidBookingStatusException;
import booking.common.exception.BookingNotFoundException;
import booking.common.exception.BookingRuleViolationException;
import booking.data.exchange.catalog.CatalogAvailabilityClient;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.messaging.publisher.NotificationPublisher;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateBookingUseCaseImplTest {

    private final BookingRepository repository = mock(BookingRepository.class);
    private final CatalogAvailabilityClient catalogAvailabilityClient =
            mock(CatalogAvailabilityClient.class);
    private final NotificationPublisher notificationPublisher =
            mock(NotificationPublisher.class);
    private final UpdateBookingUseCaseImpl useCase =
            new UpdateBookingUseCaseImpl(
                    repository,
                    catalogAvailabilityClient,
                    notificationPublisher
            );

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
    void keepsBookingInPreparationUntilUseStarts() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        Booking booking = booking(
                BookingStatus.IN_PREPARATION,
                start,
                start.plusHours(1)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.advanceAutomatically(1L);

        assertEquals(BookingStatus.IN_PREPARATION, booking.getStatus());
    }

    @Test
    void advancesPreparationToInUseWhenUseStarts() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        Booking booking = booking(
                BookingStatus.IN_PREPARATION,
                start,
                start.plusHours(1)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.advanceAutomatically(1L);

        assertEquals(BookingStatus.IN_USE, booking.getStatus());
    }

    @Test
    void keepsBookingInUseAfterEndTimeUntilItIsReturned() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = booking(
                BookingStatus.IN_USE,
                now.minusHours(1),
                now.minusSeconds(1)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        useCase.advanceAutomatically(1L);

        assertEquals(BookingStatus.IN_USE, booking.getStatus());
        verify(repository, never()).save(booking);
        verifyNoInteractions(catalogAvailabilityClient);
    }

    @Test
    void cancelsPreparationBookingAndReleasesCapacity() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        Booking booking = booking(
                BookingStatus.IN_PREPARATION,
                start,
                start.plusHours(1)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.execute(1L, BookingStatus.CANCELLED);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(catalogAvailabilityClient).release(7L, "booking:1");
    }

    @Test
    void publishesPreparationCommandWhenBookingIsApproved() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        Booking booking = booking(BookingStatus.REQUESTED, start, start.plusHours(1));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.execute(1L, BookingStatus.APPROVED);

        assertEquals(BookingStatus.APPROVED, booking.getStatus());
        verify(notificationPublisher).publishBookingApproved(booking);
    }

    @Test
    void adminCanMoveApprovedBookingToPreparation() {
        Booking booking = booking(
                BookingStatus.APPROVED,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.forceUpdateStatus(1L, BookingStatus.IN_PREPARATION);

        assertEquals(BookingStatus.IN_PREPARATION, booking.getStatus());
        verify(notificationPublisher, never()).publishBookingApproved(booking);
        verify(notificationPublisher, never()).publishBookingReturned(booking);
    }

    @Test
    void adminCanMovePreparationBookingToInUse() {
        Booking booking = booking(
                BookingStatus.IN_PREPARATION,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.forceUpdateStatus(1L, BookingStatus.IN_USE);

        assertEquals(BookingStatus.IN_USE, booking.getStatus());
        verifyNoInteractions(notificationPublisher, catalogAvailabilityClient);
    }

    @Test
    void adminCanReturnInUseBookingAndPublishesVoucherCommand() {
        Booking booking = booking(
                BookingStatus.IN_USE,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.forceUpdateStatus(1L, BookingStatus.RETURNED);

        assertEquals(BookingStatus.RETURNED, booking.getStatus());
        verify(notificationPublisher).publishBookingReturned(booking);
        verify(catalogAvailabilityClient).release(7L, "booking:1");
    }

    @Test
    void adminCanApproveBookingAndPublishesPreparationCommand() {
        Booking booking = booking(
                BookingStatus.REQUESTED,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.forceUpdateStatus(1L, BookingStatus.APPROVED);

        verify(notificationPublisher).publishBookingApproved(booking);
    }

    @Test
    void adminUpdateFailsWhenBookingDoesNotExist() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(
                BookingNotFoundException.class,
                () -> useCase.forceUpdateStatus(1L, BookingStatus.IN_PREPARATION)
        );
    }

    @Test
    void adminUpdateRejectsMissingStatus() {
        Booking booking = booking(
                BookingStatus.APPROVED,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertThrows(
                BookingRuleViolationException.class,
                () -> useCase.forceUpdateStatus(1L, null)
        );
    }

    @Test
    void normalStatusEndpointStillRejectsLifecycleTransition() {
        Booking booking = booking(
                BookingStatus.APPROVED,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertThrows(
                InvalidBookingStatusException.class,
                () -> useCase.execute(1L, BookingStatus.IN_PREPARATION)
        );

        assertEquals(BookingStatus.APPROVED, booking.getStatus());
        verify(repository, never()).save(booking);
    }

    @Test
    void publishesVoucherCommandWhenInUseBookingIsReturned() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = booking(
                BookingStatus.IN_USE,
                now.minusMinutes(30),
                now.plusMinutes(30)
        );
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(repository.save(booking)).thenReturn(booking);

        useCase.execute(1L, BookingStatus.RETURNED);

        verify(notificationPublisher).publishBookingReturned(booking);
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
        verifyNoInteractions(notificationPublisher);
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
