package booking.data.repository;

import booking.data.model.BookingEntity;
import booking.domain.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BookingJpaRepository
        extends JpaRepository<BookingEntity, Long>,
        JpaSpecificationExecutor<BookingEntity> {

    @Query("""
        SELECT COUNT(b) > 0
        FROM BookingEntity b
        WHERE b.resourceId = :resourceId
          AND b.startTime < :end
          AND b.endTime > :start
          AND b.status NOT IN (
              booking.domain.model.BookingStatus.CANCELLED,
              booking.domain.model.BookingStatus.RETURNED
          )
    """)
    boolean existsConflict(
            @Param("resourceId") Long resourceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}