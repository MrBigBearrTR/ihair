package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Salon;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SalonResponseDto {
    private final Long id;
    private final String name;
    private final String address;
    private final String phone;
    private final String email;
    private final Boolean active;
    private final LocalDateTime createdAt;

    public SalonResponseDto(Salon salon) {
        this.id = salon.getId();
        this.name = salon.getName();
        this.address = salon.getAddress();
        this.phone = salon.getPhone();
        this.email = salon.getEmail();
        this.active = salon.getActive();
        this.createdAt = salon.getCreatedAt();
    }
}
