package booking.presentation.request;

import booking.domain.model.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingStatusRequest {

    @NotNull
    private BookingStatus status;
}
