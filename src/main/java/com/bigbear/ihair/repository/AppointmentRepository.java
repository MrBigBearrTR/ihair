package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.Appointment;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByStatusNot(AppointmentStatus status);

    List<Appointment> findAllByEmployeeSalonIdAndStatusNot(Long salonId, AppointmentStatus status);

    List<Appointment> findAllByEmployeeSalonIdInAndStatusNot(Iterable<Long> salonIds, AppointmentStatus status);

    List<Appointment> findAllByEmployeeIdAndStatusNot(Long employeeId, AppointmentStatus status);

    List<Appointment> findAllByEmployeeSalonIdInOrderByAppointmentDateTimeDesc(Iterable<Long> salonIds);

    List<Appointment> findAllByEmployeeIdOrderByAppointmentDateTimeDesc(Long employeeId);

    List<Appointment> findAllByEmployeeIdAndStatusIn(Long employeeId, List<AppointmentStatus> statuses);

    List<Appointment> findAllByEmployeeSalonIdAndAppointmentDateTimeGreaterThanEqualAndAppointmentDateTimeLessThanAndStatusNotOrderByAppointmentDateTimeAsc(
            Long salonId,
            LocalDateTime start,
            LocalDateTime end,
            AppointmentStatus status);

    List<Appointment> findAllByEmployeeIdAndAppointmentDateTimeGreaterThanEqualAndAppointmentDateTimeLessThanAndStatusNotOrderByAppointmentDateTimeAsc(
            Long employeeId,
            LocalDateTime start,
            LocalDateTime end,
            AppointmentStatus status);

    @Query("""
            select a from Appointment a
            where a.employee.salon.id = :salonId
              and a.status in :statuses
              and not exists (
                  select s.id from Sale s where s.sourceAppointment = a
              )
            order by a.appointmentDateTime desc
            """)
    List<Appointment> findAvailableForSale(
            @Param("salonId") Long salonId,
            @Param("statuses") List<AppointmentStatus> statuses);
}
