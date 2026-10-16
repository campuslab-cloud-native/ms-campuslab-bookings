package booking.domain.service;

import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import booking.domain.usecase.UpdateBookingStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingLifecycleScheduler {

    private final BookingRepository repository;
    private final UpdateBookingStatusUseCase updateBookingStatusUseCase;

    @Scheduled(fixedDelayString = "${bookings.lifecycle.fixed-delay-ms:10000}")
    public void advanceBookings() {
        List<Long> bookingIds = repository.findByStatuses(List.of(
                        BookingStatus.APPROVED,
                        BookingStatus.IN_PREPARATION,
                        BookingStatus.READY,
                        BookingStatus.IN_USE
                ))
                .stream()
                .map(booking -> booking.getId())
                .toList();

        for (Long bookingId : bookingIds) {
            try {
                updateBookingStatusUseCase.advanceAutomatically(bookingId);
            } catch (RuntimeException exception) {
                log.error(
                        "Could not advance booking lifecycle. bookingId={}",
                        bookingId,
                        exception
                );
            }
        }
    }
}
