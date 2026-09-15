package booking.presentation.mapper;

import booking.domain.model.Booking;
import booking.presentation.request.CreateBookingRequest;
import booking.presentation.response.BookingResponse;
import org.springframework.stereotype.Component;

@Component
public class BookingPresentationMapper {

    public Booking toDomain(CreateBookingRequest request) {
        return Booking.builder()
                .resourceId(request.getResourceId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }

    public BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .resourceId(booking.getResourceId())
                .userId(booking.getUserId())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
