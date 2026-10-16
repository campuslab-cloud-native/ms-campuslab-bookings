package booking.common.exception;

public class BookingResourceNotFoundException extends RuntimeException {

    public BookingResourceNotFoundException(Long id) {
        super("No existe el recurso " + id);
    }
}
