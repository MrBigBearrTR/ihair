package com.bigbear.ihair.dto.request;

import lombok.Getter;

@Getter
public class CustomerRequestDto {
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String notes;
    private Long salonId;
}
