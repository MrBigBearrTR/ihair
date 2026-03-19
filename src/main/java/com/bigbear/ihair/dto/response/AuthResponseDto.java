package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private Role role;
    private LocalDateTime expiresAt;
}
