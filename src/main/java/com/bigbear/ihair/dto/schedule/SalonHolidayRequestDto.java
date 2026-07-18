package com.bigbear.ihair.dto.schedule;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class SalonHolidayRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
