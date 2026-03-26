package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.ChangePasswordRequestDto;
import com.bigbear.ihair.dto.request.LoginRequestDto;
import com.bigbear.ihair.dto.request.RefreshTokenRequestDto;
import com.bigbear.ihair.dto.request.RegisterRequestDto;
import com.bigbear.ihair.dto.response.AuthResponseDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    AuthResponseDto refresh(RefreshTokenRequestDto request);

    void logout(RefreshTokenRequestDto request);

    void changePassword(String username, ChangePasswordRequestDto request);
}
