package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.schedule.*;

import java.time.LocalDateTime;
import java.util.List;

public interface SalonScheduleService {
    SalonScheduleResponseDto getSchedule(Long salonId);
    SalonScheduleResponseDto updateSchedule(Long salonId, SalonScheduleRequestDto request);
    List<SalonHolidayDto> getHolidays(Long salonId);
    SalonHolidayDto createHoliday(Long salonId, SalonHolidayRequestDto request);
    SalonHolidayDto updateHoliday(Long salonId, Long holidayId, SalonHolidayRequestDto request);
    void deleteHoliday(Long salonId, Long holidayId);
    void validateAppointment(Long salonId, LocalDateTime start, LocalDateTime end);
    String getTimeZone(Long salonId);
}
