package com.bigbear.ihair.dto.schedule;

import lombok.Getter;

import java.util.List;

@Getter
public class SalonScheduleRequestDto {
    private String timeZone;
    private List<ScheduleDayDto> days;
}
