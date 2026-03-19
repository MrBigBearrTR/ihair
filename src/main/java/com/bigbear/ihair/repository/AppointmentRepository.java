package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.Appointment;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByStatusNot(AppointmentStatus status);

    boolean existsByEmployeeIdAndAppointmentDateTimeAndStatusIn(
            Long employeeId,
            LocalDateTime appointmentDateTime,
            List<AppointmentStatus> statuses
    );

    boolean existsByEmployeeIdAndAppointmentDateTimeAndStatusInAndIdNot(
            Long employeeId,
            LocalDateTime appointmentDateTime,
            List<AppointmentStatus> statuses,
            Long excludeId
    );
}
