package booking.presentation.controller;

import booking.domain.model.BookingStatus;
import booking.domain.usecase.CreateBookingUseCase;
import booking.domain.usecase.GetBookingByIdUseCase;
import booking.domain.usecase.GetBookingsUseCase;
import booking.domain.usecase.UpdateBookingStatusUseCase;
import booking.presentation.mapper.BookingPresentationMapper;
import booking.presentation.request.CreateBookingRequest;
import booking.presentation.request.UpdateBookingStatusRequest;
import booking.presentation.response.BookingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final GetBookingByIdUseCase getBookingByIdUseCase;
    private final GetBookingsUseCase getBookingsUseCase;
    private final UpdateBookingStatusUseCase updateBookingStatusUseCase;
    private final BookingPresentationMapper bookingPresentationMapper;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestParam(required = false) String userId,
            @Valid @RequestBody CreateBookingRequest request) {

        var booking = bookingPresentationMapper.toDomain(request);
        booking.setUserId(userId);

        var createdBooking = createBookingUseCase.execute(booking);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingPresentationMapper.toResponse(createdBooking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id) {

        var booking = getBookingByIdUseCase.execute(id);

        return ResponseEntity.ok(
                bookingPresentationMapper.toResponse(booking)
        );
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {

        var bookings = getBookingsUseCase.execute(status, from, to)
                .stream()
                .map(bookingPresentationMapper::toResponse)
                .toList();

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByUserId(
            @PathVariable String userId) {

        var bookings = getBookingsUseCase.executeByUserId(userId)
                .stream()
                .map(bookingPresentationMapper::toResponse)
                .toList();

        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {

        var booking = updateBookingStatusUseCase.execute(
                id,
                request.getStatus()
        );

        return ResponseEntity.ok(
                bookingPresentationMapper.toResponse(booking)
        );
    }
}
