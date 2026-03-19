package com.bigbear.ihair.dto.request;

import com.bigbear.ihair.entity.enums.DiscountType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CampaignRequestDto {
    private String name;
    private String description;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer maxUsageCount;
    private Boolean isCustomerSpecific;
    private Long customerId;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
