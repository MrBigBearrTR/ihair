package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.enums.PaymentMethod;
import com.bigbear.ihair.entity.enums.RevenueGroupBy;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class RevenueReportResponseDto {
    private Long salonId;
    private LocalDate from;
    private LocalDate to;
    private RevenueGroupBy groupBy;
    private BigDecimal totalRevenue;
    private long saleCount;
    private long itemCount;
    private BigDecimal averageSale;
    private Map<PaymentMethod, BigDecimal> paymentBreakdown;
    private List<RevenueGroupResponseDto> groups;
}
