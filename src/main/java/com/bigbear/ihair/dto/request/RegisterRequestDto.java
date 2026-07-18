package com.bigbear.ihair.dto.request;

import com.bigbear.ihair.entity.enums.Role;
import lombok.Getter;

import java.util.Set;

@Getter
public class RegisterRequestDto {
    private String username;
    private String firstName;
    private String lastName;
    private String password;
    private Role role;
    private Long salonId;
    private Set<Long> salonIds;
    private Long defaultSalonId;
    private Long employeeId;
}
