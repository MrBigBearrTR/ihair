package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Employee;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EmployeeResponseDto {
    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String email;
    private final Boolean active;
    private final Long salonId;
    private final String salonName;
    private final LocalDateTime createdAt;

    public EmployeeResponseDto(Employee employee) {
        this.id = employee.getId();
        this.firstName = employee.getFirstName();
        this.lastName = employee.getLastName();
        this.phone = employee.getPhone();
        this.email = employee.getEmail();
        this.active = employee.getActive();
        this.salonId = employee.getSalon().getId();
        this.salonName = employee.getSalon().getName();
        this.createdAt = employee.getCreatedAt();
    }
}
