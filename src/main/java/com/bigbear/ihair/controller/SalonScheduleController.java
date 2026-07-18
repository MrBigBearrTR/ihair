package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.schedule.*;
import com.bigbear.ihair.service.SalonScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salons/{salonId}")
@RequiredArgsConstructor
public class SalonScheduleController {

    private final SalonScheduleService scheduleService;

    @GetMapping("/schedule")
    public SalonScheduleResponseDto getSchedule(@PathVariable Long salonId) {
        return scheduleService.getSchedule(salonId);
    }

    @PutMapping("/schedule")
    public SalonScheduleResponseDto updateSchedule(
            @PathVariable Long salonId,
            @RequestBody SalonScheduleRequestDto request) {
        return scheduleService.updateSchedule(salonId, request);
    }

    @GetMapping("/holidays")
    public List<SalonHolidayDto> getHolidays(@PathVariable Long salonId) {
        return scheduleService.getHolidays(salonId);
    }

    @PostMapping("/holidays")
    public ResponseEntity<SalonHolidayDto> createHoliday(
            @PathVariable Long salonId,
            @RequestBody SalonHolidayRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.createHoliday(salonId, request));
    }

    @PutMapping("/holidays/{holidayId}")
    public SalonHolidayDto updateHoliday(
            @PathVariable Long salonId,
            @PathVariable Long holidayId,
            @RequestBody SalonHolidayRequestDto request) {
        return scheduleService.updateHoliday(salonId, holidayId, request);
    }

    @DeleteMapping("/holidays/{holidayId}")
    public ResponseEntity<Void> deleteHoliday(
            @PathVariable Long salonId,
            @PathVariable Long holidayId) {
        scheduleService.deleteHoliday(salonId, holidayId);
        return ResponseEntity.noContent().build();
    }
}
