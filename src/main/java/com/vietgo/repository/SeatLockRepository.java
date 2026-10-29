package com.vietgo.repository;

import com.vietgo.domain.SeatLock;
import com.vietgo.domain.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {

    @Query("select s.seatKey from SeatLock s where s.ticketType = :type and s.tripId = :tripId")
    List<String> findSeatKeys(@Param("type") TicketType type, @Param("tripId") Long tripId);

    long countByTicketTypeAndTripId(TicketType type, Long tripId);

    @Query("select s.seatKey from SeatLock s where s.ticketType = :type and s.tripId = :tripId and s.seatKey in :keys")
    List<String> findTaken(@Param("type") TicketType type, @Param("tripId") Long tripId,
                           @Param("keys") Collection<String> keys);

    @Modifying
    @Query("delete from SeatLock s where s.bookingId = :bookingId")
    int deleteByBookingId(@Param("bookingId") Long bookingId);
}
