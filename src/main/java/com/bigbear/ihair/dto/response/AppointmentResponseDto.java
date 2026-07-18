package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Appointment;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class AppointmentResponseDto {
    private final Long id;
    private final Long salonId;
    private final Long customerId;
    private final String customerName;
    private final Long employeeId;
    private final String employeeName;
    private final Long hairServiceId;
    private final String hairServiceName;
    private final BigDecimal hairServicePrice;
    private final LocalDateTime appointmentDateTime;
    private final Integer durationMinutes;
    private final LocalDateTime endsAt;
    private final AppointmentStatus status;
    private final String notes;
    private final String campaignCode;
    private final BigDecimal finalPrice;
    private final Long saleId;
    private final Boolean scheduleOverridden;
    private final String scheduleOverrideReason;
    private final Long scheduleOverrideById;
    private final LocalDateTime scheduleOverrideAt;
    private final Long version;
    private final LocalDateTime createdAt;

    public AppointmentResponseDto(Appointment appointment) {
        this.id = appointment.getId();
        this.salonId = appointment.getSalon() != null
                ? appointment.getSalon().getId()
                : appointment.getEmployee().getSalon().getId();
        this.customerId = appointment.getCustomer().getId();
        this.customerName = appointment.getCustomer().getFirstName() + " " + appointment.getCustomer().getLastName();
        this.employeeId = appointment.getEmployee().getId();
        this.employeeName = appointment.getEmployee().getFirstName() + " " + appointment.getEmployee().getLastName();
        this.hairServiceId = appointment.getHairService().getId();
        this.hairServiceName = appointment.getHairService().getName();
        this.hairServicePrice = appointment.getHairService().getPrice();
        this.appointmentDateTime = appointment.getAppointmentDateTime();
        this.durationMinutes = appointment.getDurationMinutesSnapshot() != null
                ? appointment.getDurationMinutesSnapshot()
                : appointment.getHairService().getDurationMinutes();
        this.endsAt = appointment.getEndsAt() != null
                ? appointment.getEndsAt()
                : appointment.getAppointmentDateTime().plusMinutes(this.durationMinutes);
        this.status = appointment.getStatus();
        this.notes = appointment.getNotes();
        this.campaignCode = appointment.getCampaign() != null ? appointment.getCampaign().getCode() : null;
        this.finalPrice = appointment.getFinalPrice();
        this.saleId = appointment.getSale() != null ? appointment.getSale().getId() : null;
        this.scheduleOverridden = Boolean.TRUE.equals(appointment.getScheduleOverridden());
        this.scheduleOverrideReason = appointment.getScheduleOverrideReason();
        this.scheduleOverrideById = appointment.getScheduleOverrideBy() != null
                ? appointment.getScheduleOverrideBy().getId() : null;
        this.scheduleOverrideAt = appointment.getScheduleOverrideAt();
        this.version = appointment.getVersion();
        this.createdAt = appointment.getCreatedAt();
    }
}
