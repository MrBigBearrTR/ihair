package com.bigbear.ihair.dto.request;

import lombok.Getter;

@Getter
public class EmployeeRequestDto {
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private Long salonId;
}
