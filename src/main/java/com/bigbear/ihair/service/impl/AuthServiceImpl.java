package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.LoginRequestDto;
import com.bigbear.ihair.dto.request.RefreshTokenRequestDto;
import com.bigbear.ihair.dto.request.RegisterRequestDto;
import com.bigbear.ihair.dto.response.AuthResponseDto;
import com.bigbear.ihair.entity.RefreshToken;
import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.RefreshTokenRepository;
import com.bigbear.ihair.repository.UserRepository;
import com.bigbear.ihair.security.JwtService;
import com.bigbear.ihair.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Bu kullanıcı adı zaten alınmış: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDto refresh(RefreshTokenRequestDto request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Geçersiz refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new RuntimeException("Refresh token süresi dolmuş, lütfen tekrar giriş yapın");
        }

        return buildAuthResponse(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }

    private AuthResponseDto buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        LocalDateTime expiresAt = jwtService.extractExpiration(accessToken);

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        refreshTokenRepository.save(refreshToken);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .role(user.getRole())
                .expiresAt(expiresAt)
                .build();
    }
}
