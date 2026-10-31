package com.vietgo.repository;

import com.vietgo.domain.BusTrip;
import com.vietgo.domain.BusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BusTripRepository extends JpaRepository<BusTrip, Long> {

    @Query("""
            select b from BusTrip b
            where b.active = true
              and b.originCity = :origin
              and b.destinationCity = :destination
              and b.departureTime >= :from and b.departureTime < :to
              and (:busType is null or b.busType = :busType)
            order by b.departureTime asc
            """)
    List<BusTrip> search(@Param("origin") String origin,
                         @Param("destination") String destination,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         @Param("busType") BusType busType);

    @Query("select distinct b.originCity from BusTrip b order by b.originCity")
    List<String> findAllOrigins();

    @Query("select distinct b.destinationCity from BusTrip b order by b.destinationCity")
    List<String> findAllDestinations();

    List<BusTrip> findAllByOrderByDepartureTimeDesc();

    List<BusTrip> findByDepartureTimeBetweenOrderByDepartureTimeAsc(LocalDateTime from, LocalDateTime to);

    @Query("select distinct b.originCity, b.destinationCity from BusTrip b " +
           "where b.active = true and b.departureTime > :now order by b.originCity, b.destinationCity")
    List<Object[]> findActiveRoutePairs(@Param("now") LocalDateTime now);

    /** Cac chuyen sap chay gan nhat (bang gio khoi hanh o trang chu). */
    List<BusTrip> findTop4ByActiveTrueAndDepartureTimeAfterOrderByDepartureTimeAsc(LocalDateTime after);

    @Query("select b from BusTrip b where b.departureTime >= :from order by b.departureTime asc")
    List<BusTrip> findUpcoming(@Param("from") LocalDateTime from);
}
