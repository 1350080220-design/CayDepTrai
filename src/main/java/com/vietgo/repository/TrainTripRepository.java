package com.vietgo.repository;

import com.vietgo.domain.TrainTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TrainTripRepository extends JpaRepository<TrainTrip, Long> {

    @Query("""
            select t from TrainTrip t
            where t.active = true
              and t.originStation = :origin
              and t.destinationStation = :destination
              and t.departureTime >= :from and t.departureTime < :to
            order by t.departureTime asc
            """)
    List<TrainTrip> search(@Param("origin") String origin,
                           @Param("destination") String destination,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);

    @Query("select distinct t.originStation from TrainTrip t order by t.originStation")
    List<String> findAllOrigins();

    @Query("select distinct t.destinationStation from TrainTrip t order by t.destinationStation")
    List<String> findAllDestinations();

    List<TrainTrip> findAllByOrderByDepartureTimeDesc();

    List<TrainTrip> findByDepartureTimeBetweenOrderByDepartureTimeAsc(LocalDateTime from, LocalDateTime to);

    @Query("select distinct t.originStation, t.destinationStation from TrainTrip t " +
           "where t.active = true and t.departureTime > :now order by t.originStation, t.destinationStation")
    List<Object[]> findActiveRoutePairs(@Param("now") LocalDateTime now);

    @Query("select t from TrainTrip t where t.departureTime >= :from order by t.departureTime asc")
    List<TrainTrip> findUpcoming(@Param("from") LocalDateTime from);

    /** Cac chuyen sap chay gan nhat (bang gio khoi hanh o trang chu). */
    List<TrainTrip> findTop4ByActiveTrueAndDepartureTimeAfterOrderByDepartureTimeAsc(LocalDateTime after);

    boolean existsByTrainCodeAndDepartureTime(String trainCode, LocalDateTime departureTime);

    boolean existsByTrainCodeAndDepartureTimeAndIdNot(String trainCode, LocalDateTime departureTime, Long id);
}
