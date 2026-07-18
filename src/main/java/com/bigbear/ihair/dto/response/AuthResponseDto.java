package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private Role role;
    private Long salonId;
    private List<Long> salonIds;
    private List<SalonResponseDto> salons;
    private Long employeeId;
    private LocalDateTime expiresAt;
}
