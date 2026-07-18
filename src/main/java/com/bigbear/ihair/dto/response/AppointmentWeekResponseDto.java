package com.bigbear.ihair.dto.response;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class AppointmentWeekResponseDto {
    private final Long salonId;
    private final String timeZone;
    private final LocalDate weekStart;
    private final LocalDate weekEndExclusive;
    private final List<AppointmentResponseDto> appointments;

    public AppointmentWeekResponseDto(
            Long salonId,
            String timeZone,
            LocalDate weekStart,
            LocalDate weekEndExclusive,
            List<AppointmentResponseDto> appointments) {
        this.salonId = salonId;
        this.timeZone = timeZone;
        this.weekStart = weekStart;
        this.weekEndExclusive = weekEndExclusive;
        this.appointments = appointments;
    }
}
