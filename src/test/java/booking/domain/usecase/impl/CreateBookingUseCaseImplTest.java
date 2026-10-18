package booking.domain.usecase.impl;

import booking.common.exception.BookingRuleViolationException;
import booking.data.exchange.catalog.CatalogAvailabilityClient;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.messaging.publisher.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateBookingUseCaseImplTest {

    private final BookingRepository repository = mock(BookingRepository.class);
    private final NotificationPublisher notificationPublisher =
            mock(NotificationPublisher.class);
    private final CatalogAvailabilityClient catalogAvailabilityClient =
            mock(CatalogAvailabilityClient.class);
    private final CreateBookingUseCaseImpl useCase =
            new CreateBookingUseCaseImpl(
                    repository,
                    notificationPublisher,
                    catalogAvailabilityClient
            );

    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = Booking.builder()
                .resourceId(7L)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
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
                        catalogAvailabilityClient
                );

        Booking created = useCaseWithUnavailableBroker.execute(booking);

        assertNotNull(created.getId());
        verify(catalogAvailabilityClient).reserve(7L);
        verify(catalogAvailabilityClient, org.mockito.Mockito.never())
                .release(any(Long.class), any(String.class));
    }
}
