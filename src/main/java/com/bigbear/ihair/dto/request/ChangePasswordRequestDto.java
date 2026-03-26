package com.bigbear.ihair.dto.request;

import lombok.Getter;

@Getter
public class ChangePasswordRequestDto {
    private String currentPassword;
    private String newPassword;
}
