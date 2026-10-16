package booking.common.exception;

public class BookingRuleViolationException extends RuntimeException {

    public BookingRuleViolationException(String message) {
        super(message);
    }
}
