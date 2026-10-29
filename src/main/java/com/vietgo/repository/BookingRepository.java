package com.vietgo.repository;

import com.vietgo.domain.Booking;
import com.vietgo.domain.BookingStatus;
import com.vietgo.domain.TicketType;
import com.vietgo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByCode(String code);

    boolean existsByCode(String code);

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime time);

    List<Booking> findAllByOrderByCreatedAtDesc();

    long countByStatus(BookingStatus status);

    long countByTicketTypeAndStatus(TicketType type, BookingStatus status);

    long countByTrainTripId(Long tripId);

    long countByBusTripId(Long tripId);

    @Query("select b from Booking b where b.paidAt is not null and b.paidAt >= :from")
    List<Booking> findPaidSince(@Param("from") LocalDateTime from);

    @Query("select b from Booking b where b.paidAt is not null")
    List<Booking> findAllPaid();
}
