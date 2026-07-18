package com.bigbear.ihair.dto.schedule;

import com.bigbear.ihair.entity.SalonScheduleDay;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ScheduleDayDto {
    private DayOfWeek dayOfWeek;
    private Boolean closed;
    private LocalTime opensAt;
    private LocalTime closesAt;

    public ScheduleDayDto(SalonScheduleDay day) {
        this(day.getDayOfWeek(), day.getClosed(), day.getOpensAt(), day.getClosesAt());
    }

    public ScheduleDayDto(DayOfWeek dayOfWeek, Boolean closed, LocalTime opensAt, LocalTime closesAt) {
        this.dayOfWeek = dayOfWeek;
        this.closed = closed;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }
}
