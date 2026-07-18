package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.response.RevenueReportResponseDto;
import com.bigbear.ihair.entity.enums.RevenueGroupBy;
import com.bigbear.ihair.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReportResponseDto> getRevenue(
            @RequestParam(required = false) Long salonId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam RevenueGroupBy groupBy,
            @RequestParam(required = false) Long employeeId) {
        return ResponseEntity.ok(reportService.getRevenue(salonId, from, to, groupBy, employeeId));
    }
}
