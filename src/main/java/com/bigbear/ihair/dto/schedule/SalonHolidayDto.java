package com.bigbear.ihair.dto.schedule;

import com.bigbear.ihair.entity.SalonHoliday;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SalonHolidayDto {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;

    public SalonHolidayDto(SalonHoliday holiday) {
        this.id = holiday.getId();
        this.startDate = holiday.getStartDate();
        this.endDate = holiday.getEndDate();
        this.reason = holiday.getReason();
    }
}
