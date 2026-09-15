package booking.common.exception;

public class BookingNotFoundException extends RuntimeException{

    public BookingNotFoundException(Long id) {
        super("Resource not found with id: " + id);
    }
}
