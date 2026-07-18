package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.AppointmentRequestDto;
import com.bigbear.ihair.dto.request.AppointmentStatusRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;
import com.bigbear.ihair.dto.response.AppointmentWeekResponseDto;
import com.bigbear.ihair.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDto>> getAll(
            @RequestParam(required = false) Long salonId) {
        return ResponseEntity.ok(appointmentService.getAll(salonId));
    }

    @GetMapping("/week")
    public ResponseEntity<AppointmentWeekResponseDto> getWeek(
            @RequestParam(required = false) Long salonId,
            @RequestParam LocalDate weekStart) {
        return ResponseEntity.ok(appointmentService.getWeek(salonId, weekStart));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> create(@RequestBody AppointmentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> update(@PathVariable Long id, @RequestBody AppointmentRequestDto request) {
        return ResponseEntity.ok(appointmentService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody AppointmentStatusRequestDto request) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
