package com.vietgo.repository;

import com.vietgo.domain.Consultation;
import com.vietgo.domain.ConsultationStatus;
import com.vietgo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByUserOrderByAppointmentTimeDesc(User user);

    List<Consultation> findAllByOrderByAppointmentTimeDesc();

    long countByAppointmentTimeAndStatusIn(LocalDateTime time, Collection<ConsultationStatus> statuses);

    long countByUserAndAppointmentTimeAfterAndStatusIn(User user, LocalDateTime time,
                                                       Collection<ConsultationStatus> statuses);

    boolean existsByUserAndAppointmentTimeAndStatusIn(User user, LocalDateTime time,
                                                      Collection<ConsultationStatus> statuses);

    @Query("select c.appointmentTime from Consultation c where c.status in :statuses " +
           "and c.appointmentTime >= :from and c.appointmentTime < :to")
    List<LocalDateTime> findBookedTimes(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                        @Param("statuses") Collection<ConsultationStatus> statuses);

    long countByStatus(ConsultationStatus status);
}
