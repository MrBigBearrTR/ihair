package com.bigbear.ihair.dto.request;

import com.bigbear.ihair.entity.enums.AppointmentStatus;
import lombok.Getter;

@Getter
public class AppointmentStatusRequestDto {
    private AppointmentStatus status;
}
