package com.bigbear.ihair.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RevenueGroupResponseDto {
    private String key;
    private String label;
    private Long employeeId;
    private BigDecimal revenue;
    private long saleCount;
    private long itemCount;
}
