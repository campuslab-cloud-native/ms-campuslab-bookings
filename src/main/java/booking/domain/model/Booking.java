package booking.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private Long id;
    private Long resourceId;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private LocalDateTime createdAt;
}
