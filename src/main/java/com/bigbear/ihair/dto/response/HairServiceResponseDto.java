package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.HairService;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class HairServiceResponseDto {
    private final Long id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final Integer durationMinutes;
    private final Boolean active;
    private final Long salonId;
    private final String salonName;
    private final LocalDateTime createdAt;

    public HairServiceResponseDto(HairService hairService) {
        this.id = hairService.getId();
        this.name = hairService.getName();
        this.description = hairService.getDescription();
        this.price = hairService.getPrice();
        this.durationMinutes = hairService.getDurationMinutes();
        this.active = hairService.getActive();
        this.salonId = hairService.getSalon().getId();
        this.salonName = hairService.getSalon().getName();
        this.createdAt = hairService.getCreatedAt();
    }
}
