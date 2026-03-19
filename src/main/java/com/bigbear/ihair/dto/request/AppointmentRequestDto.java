package com.bigbear.ihair.dto.request;

import com.bigbear.ihair.entity.enums.AppointmentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AppointmentRequestDto {
    private Long customerId;
    private Long employeeId;
    private Long hairServiceId;
    private LocalDateTime appointmentDateTime;
    private AppointmentStatus status;
    private String notes;
    private String campaignCode;
}
