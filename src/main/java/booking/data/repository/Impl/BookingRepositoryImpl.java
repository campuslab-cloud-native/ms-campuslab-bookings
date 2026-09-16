package booking.data.repository.Impl;

import booking.data.mapper.BookingDataMapper;
import booking.data.model.BookingEntity;
import booking.data.repository.BookingJpaRepository;
import booking.domain.model.Booking;
import booking.domain.model.BookingStatus;
import booking.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {

    private final BookingJpaRepository bookingJpaRepository;
    private final BookingDataMapper bookingDataMapper;

    @Override
    public Booking save(Booking booking) {
        BookingEntity bookingEntity = bookingDataMapper.toEntity(booking);
        BookingEntity savedEntity = bookingJpaRepository.save(bookingEntity);
        return bookingDataMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return bookingJpaRepository.findById(id)
                .map(bookingDataMapper::toDomain);
    }

    @Override
    public List<Booking> findByFilters(
            BookingStatus status,
            LocalDate from,
            LocalDate to
    ) {

        Specification<BookingEntity> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (status != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("status"),
                                    status
                            )
            );
        }

        if (from != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.greaterThanOrEqualTo(
                                    root.get("startTime"),
                                    from.atStartOfDay()
                            )
            );
        }

        if (to != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.lessThanOrEqualTo(
                                    root.get("startTime"),
                                    to.atStartOfDay()
                            )
            );
        }

        return bookingJpaRepository.findAll(specification)
                .stream()
                .map(bookingDataMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsConflict(
            Long resourceId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return bookingJpaRepository.existsConflict(
                resourceId,
                start,
                end
        );
    }
}