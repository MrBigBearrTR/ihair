package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Appointment;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class AppointmentResponseDto {
    private final Long id;
    private final Long customerId;
    private final String customerName;
    private final Long employeeId;
    private final String employeeName;
    private final Long hairServiceId;
    private final String hairServiceName;
    private final BigDecimal hairServicePrice;
    private final LocalDateTime appointmentDateTime;
    private final AppointmentStatus status;
    private final String notes;
    private final String campaignCode;
    private final BigDecimal finalPrice;
    private final LocalDateTime createdAt;

    public AppointmentResponseDto(Appointment appointment) {
        this.id = appointment.getId();
        this.customerId = appointment.getCustomer().getId();
        this.customerName = appointment.getCustomer().getFirstName() + " " + appointment.getCustomer().getLastName();
        this.employeeId = appointment.getEmployee().getId();
        this.employeeName = appointment.getEmployee().getFirstName() + " " + appointment.getEmployee().getLastName();
        this.hairServiceId = appointment.getHairService().getId();
        this.hairServiceName = appointment.getHairService().getName();
        this.hairServicePrice = appointment.getHairService().getPrice();
        this.appointmentDateTime = appointment.getAppointmentDateTime();
        this.status = appointment.getStatus();
        this.notes = appointment.getNotes();
        this.campaignCode = appointment.getCampaign() != null ? appointment.getCampaign().getCode() : null;
        this.finalPrice = appointment.getFinalPrice();
        this.createdAt = appointment.getCreatedAt();
    }
}
