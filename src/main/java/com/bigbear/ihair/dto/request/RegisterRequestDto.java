package com.bigbear.ihair.dto.request;

import com.bigbear.ihair.entity.enums.Role;
import lombok.Getter;

@Getter
public class RegisterRequestDto {
    private String username;
    private String firstName;
    private String lastName;
    private String password;
    private Role role;
}
