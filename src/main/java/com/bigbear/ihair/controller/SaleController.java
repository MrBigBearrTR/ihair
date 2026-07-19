package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.CompleteSaleRequestDto;
import com.bigbear.ihair.dto.request.SaleRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;
import com.bigbear.ihair.dto.response.PagedResponseDto;
import com.bigbear.ihair.dto.response.SaleListResponseDto;
import com.bigbear.ihair.dto.response.SaleResponseDto;
import com.bigbear.ihair.dto.response.SaleQuoteResponseDto;
import com.bigbear.ihair.entity.enums.SaleStatus;
import com.bigbear.ihair.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER', 'EMPLOYEE')")
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<List<SaleResponseDto>> getAll(
            @RequestParam(required = false) Long salonId,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long employeeId) {
        return ResponseEntity.ok(saleService.getAll(salonId, status, from, to, employeeId));
    }

    @GetMapping("/paged")
    public ResponseEntity<PagedResponseDto<SaleListResponseDto>> getPaged(
            @RequestParam(required = false) Long salonId,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(
                saleService.getPaged(salonId, status, from, to, employeeId, page, size));
    }

    public ResponseEntity<List<SaleResponseDto>> getAll(Long salonId, SaleStatus status) {
        return ResponseEntity.ok(saleService.getAll(salonId, status));
    }

    @GetMapping("/available-appointments")
    public ResponseEntity<List<AppointmentResponseDto>> getAvailableAppointments(
            @RequestParam(required = false) Long salonId) {
        return ResponseEntity.ok(saleService.getAvailableAppointments(salonId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getById(id));
    }

    @PostMapping("/quote")
    public ResponseEntity<SaleQuoteResponseDto> quote(@RequestBody SaleRequestDto request) {
        return ResponseEntity.ok(saleService.quote(request));
    }

    @PostMapping
    public ResponseEntity<SaleResponseDto> create(@RequestBody SaleRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaleResponseDto> update(
            @PathVariable Long id,
            @RequestBody SaleRequestDto request) {
        return ResponseEntity.ok(saleService.update(id, request));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SaleResponseDto> complete(
            @PathVariable Long id,
            @RequestBody CompleteSaleRequestDto request) {
        return ResponseEntity.ok(saleService.complete(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SaleResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.cancel(id));
    }
}
