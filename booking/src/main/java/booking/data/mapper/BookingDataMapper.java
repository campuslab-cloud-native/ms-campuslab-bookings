package booking.data.mapper;

import booking.data.model.BookingEntity;
import booking.domain.model.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingDataMapper {

    public BookingEntity toEntity(Booking booking) {
        return BookingEntity.builder()
                .id(booking.getId())
                .resourceId(booking.getResourceId())
                .userId(booking.getUserId())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    public Booking toDomain(BookingEntity bookingEntity) {
        return Booking.builder()
                .id(bookingEntity.getId())
                .resourceId(bookingEntity.getResourceId())
                .userId(bookingEntity.getUserId())
                .startTime(bookingEntity.getStartTime())
                .endTime(bookingEntity.getEndTime())
                .status(bookingEntity.getStatus())
                .createdAt(bookingEntity.getCreatedAt())
                .build();
    }

}
