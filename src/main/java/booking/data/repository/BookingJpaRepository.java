package booking.data.repository;

import booking.data.model.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingJpaRepository
extends JpaRepository<BookingEntity, Long>,
        JpaSpecificationExecutor<BookingEntity> {

}
