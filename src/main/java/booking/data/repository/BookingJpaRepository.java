package booking.data.repository;

import booking.data.model.BookingEntity;
import booking.domain.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;

public interface BookingJpaRepository
        extends JpaRepository<BookingEntity, Long>,
        JpaSpecificationExecutor<BookingEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BookingEntity b WHERE b.id = :id")
    java.util.Optional<BookingEntity> findByIdForUpdate(@Param("id") Long id);

    List<BookingEntity> findByStatusIn(Collection<BookingStatus> statuses);
}