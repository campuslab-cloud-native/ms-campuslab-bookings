package booking.presentation.response;

import booking.domain.model.BookingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long resourceId;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private LocalDateTime createdAt;
}
