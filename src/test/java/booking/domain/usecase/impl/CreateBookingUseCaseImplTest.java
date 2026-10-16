package booking.domain.usecase.impl;

import booking.common.exception.BookingRuleViolationException;
import booking.data.exchange.catalog.CatalogAvailabilityClient;
import booking.domain.model.Booking;
import booking.domain.repository.BookingRepository;
import booking.messaging.model.NotificationMessage;
import booking.messaging.publisher.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void createsBookingWithoutNullableUserIdInNotificationPayload() {
        Booking created = useCase.execute(booking);

        assertNotNull(created.getId());
        ArgumentCaptor<NotificationMessage> message =
                ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationPublisher).publishEmail(message.capture());
        Map<String, Object> payload = message.getValue().getPayload();
        assertFalse(payload.containsKey("userId"));
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
    void restoresCapacityWhenNotificationPublishingFails() {
        doThrow(new AmqpException("broker unavailable"))
                .when(notificationPublisher)
                .publishEmail(any(NotificationMessage.class));

        assertThrows(AmqpException.class, () -> useCase.execute(booking));

        verify(catalogAvailabilityClient).reserve(7L);
        verify(catalogAvailabilityClient).release(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.startsWith("booking-compensation:")
        );
    }
}
