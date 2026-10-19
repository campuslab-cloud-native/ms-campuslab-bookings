package booking.domain.usecase.impl;

import booking.common.exception.BookingRuleViolationException;
import booking.config.BookingTimeConfiguration;
import booking.data.exchange.catalog.CatalogAvailabilityClient;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.messaging.publisher.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateBookingUseCaseImplTest {

    private static final ZoneId SANTIAGO = BookingTimeConfiguration.ZONE_ID;
    private final Clock bookingClock = Clock.fixed(
            Instant.parse("2026-10-17T15:00:00Z"),
            SANTIAGO
    );
    private final BookingRepository repository = mock(BookingRepository.class);
    private final NotificationPublisher notificationPublisher =
            mock(NotificationPublisher.class);
    private final CatalogAvailabilityClient catalogAvailabilityClient =
            mock(CatalogAvailabilityClient.class);
    private final CreateBookingUseCaseImpl useCase =
            new CreateBookingUseCaseImpl(
                    repository,
                    notificationPublisher,
                    catalogAvailabilityClient,
                    bookingClock
            );

    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = Booking.builder()
                .resourceId(7L)
                .startTime(LocalDateTime.now(bookingClock).plusHours(1))
                .endTime(LocalDateTime.now(bookingClock).plusHours(2))
                .build();
        when(repository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });
    }

    @Test
    void publishesRequestedCommandAfterCreatingBooking() {
        Booking created = useCase.execute(booking);

        assertNotNull(created.getId());
        org.junit.jupiter.api.Assertions.assertEquals(
                BookingStatus.REQUESTED,
                created.getStatus()
        );
        verify(notificationPublisher).publishBookingRequested(created);
        verify(catalogAvailabilityClient).reserve(7L);
    }

    @Test
    void preservesSelectedSantiagoTimeDuringDaylightSaving() {
        booking.setStartTime(LocalDateTime.parse("2026-10-18T15:00:00"));
        booking.setEndTime(LocalDateTime.parse("2026-10-18T16:00:00"));

        Booking created = useCase.execute(booking);

        org.junit.jupiter.api.Assertions.assertEquals(
                LocalDateTime.parse("2026-10-18T15:00:00"),
                created.getStartTime()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                LocalDateTime.parse("2026-10-18T16:00:00"),
                created.getEndTime()
        );
    }

    @Test
    void acceptsFutureBookingDuringStandardTimeWithoutChangingItsLocalTime() {
        Clock winterClock = Clock.fixed(
                Instant.parse("2026-07-17T13:00:00Z"),
                SANTIAGO
        );
        CreateBookingUseCaseImpl winterUseCase = new CreateBookingUseCaseImpl(
                repository,
                notificationPublisher,
                catalogAvailabilityClient,
                winterClock
        );
        booking.setStartTime(LocalDateTime.parse("2026-07-18T15:00:00"));
        booking.setEndTime(LocalDateTime.parse("2026-07-18T16:00:00"));

        Booking created = winterUseCase.execute(booking);

        org.junit.jupiter.api.Assertions.assertEquals(
                LocalDateTime.parse("2026-07-18T15:00:00"),
                created.getStartTime()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                LocalDateTime.parse("2026-07-18T16:00:00"),
                created.getEndTime()
        );
    }

    @Test
    void rejectsBookingInThePastInSantiagoLocalTime() {
        booking.setStartTime(LocalDateTime.parse("2026-10-17T11:59:00"));
        booking.setEndTime(LocalDateTime.parse("2026-10-17T12:59:00"));

        assertThrows(
                BookingRuleViolationException.class,
                () -> useCase.execute(booking)
        );

        verifyNoInteractions(repository, notificationPublisher, catalogAvailabilityClient);
    }

    @Test
    void rejectsBookingsLongerThanTwoHoursBeforeReservingCapacity() {
        booking.setEndTime(booking.getStartTime().plusHours(2).plusMinutes(1));

        assertThrows(
                BookingRuleViolationException.class,
                () -> useCase.execute(booking)
        );

        verifyNoInteractions(repository, notificationPublisher, catalogAvailabilityClient);
    }

    @Test
    void persistsBookingWhenNotificationPublishingFails() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(
                                booking.messaging.model.NotificationMessage.class
                        )
                );
        NotificationPublisher publisher = new NotificationPublisher(rabbitTemplate);
        CreateBookingUseCaseImpl useCaseWithUnavailableBroker =
                new CreateBookingUseCaseImpl(
                        repository,
                        publisher,
                        catalogAvailabilityClient,
                        bookingClock
                );

        Booking created = useCaseWithUnavailableBroker.execute(booking);

        assertNotNull(created.getId());
        verify(catalogAvailabilityClient).reserve(7L);
        verify(catalogAvailabilityClient, org.mockito.Mockito.never())
                .release(any(Long.class), any(String.class));
    }
}
