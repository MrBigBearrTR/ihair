package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.response.RevenueReportResponseDto;
import com.bigbear.ihair.entity.enums.RevenueGroupBy;

import java.time.LocalDate;

public interface ReportService {
    RevenueReportResponseDto getRevenue(
            Long salonId,
            LocalDate from,
            LocalDate to,
            RevenueGroupBy groupBy,
            Long employeeId);
}
