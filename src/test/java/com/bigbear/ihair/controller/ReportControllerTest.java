package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.response.RevenueReportResponseDto;
import com.bigbear.ihair.entity.enums.RevenueGroupBy;
import com.bigbear.ihair.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class ReportControllerTest {

    @Test
    void revenueDelegatesOptionalEmployeeFilter() {
        ReportService service = mock(ReportService.class);
        ReportController controller = new ReportController(service);
        RevenueReportResponseDto response = mock(RevenueReportResponseDto.class);
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        when(service.getRevenue(1L, from, to, RevenueGroupBy.DAY, 7L)).thenReturn(response);

        var result = controller.getRevenue(1L, from, to, RevenueGroupBy.DAY, 7L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(service).getRevenue(1L, from, to, RevenueGroupBy.DAY, 7L);
    }
}
