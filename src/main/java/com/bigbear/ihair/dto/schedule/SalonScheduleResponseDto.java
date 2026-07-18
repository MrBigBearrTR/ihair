package com.bigbear.ihair.dto.schedule;

import lombok.Getter;

import java.util.List;

@Getter
public class SalonScheduleResponseDto {
    private final Long salonId;
    private final String timeZone;
    private final boolean configured;
    private final List<ScheduleDayDto> days;
    private final List<SalonHolidayDto> holidays;

    public SalonScheduleResponseDto(
            Long salonId,
            String timeZone,
            boolean configured,
            List<ScheduleDayDto> days,
            List<SalonHolidayDto> holidays) {
        this.salonId = salonId;
        this.timeZone = timeZone;
        this.configured = configured;
        this.days = days;
        this.holidays = holidays;
    }
}
