package com.bigbear.ihair.dto.request;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class HairServiceRequestDto {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes = 30;
    private Long salonId;
}
