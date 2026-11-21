
package booking.domain.service;

import booking.domain.model.Booking;
import booking.domain.port.BookingEventPublisherPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingKafkaEventService {

    private final BookingEventPublisherPort publisher;

    public void bookingCreated(Booking booking) {
        afterCommit(
            () -> publisher.publishBookingCreated(booking)
        );
    }

    public void bookingStatusChanged(Booking booking) {
        afterCommit(
            () -> publisher.publishBookingStatusChanged(booking)
        );
    }

    private void afterCommit(Runnable action) {

        if (!TransactionSynchronizationManager
                .isActualTransactionActive()
                || !TransactionSynchronizationManager
                .isSynchronizationActive()) {
            throw new IllegalStateException(
                "Se requiere una transaccion activa"
            );
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (RuntimeException e) {
                        log.error(
                            "Error al iniciar publicacion Kafka despues del commit",
                            e
                        );
                    }
                }
            }
        );
    }
}
