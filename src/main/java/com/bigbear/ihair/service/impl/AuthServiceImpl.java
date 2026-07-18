package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.ChangePasswordRequestDto;
import com.bigbear.ihair.dto.request.LoginRequestDto;
import com.bigbear.ihair.dto.request.RefreshTokenRequestDto;
import com.bigbear.ihair.dto.request.RegisterRequestDto;
import com.bigbear.ihair.dto.response.AuthResponseDto;
import com.bigbear.ihair.dto.response.UserResponseDto;
import com.bigbear.ihair.entity.Employee;
import com.bigbear.ihair.entity.RefreshToken;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.DuplicateResourceException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.RefreshTokenRepository;
import com.bigbear.ihair.repository.EmployeeRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.repository.UserRepository;
import com.bigbear.ihair.security.JwtService;
import com.bigbear.ihair.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final SalonRepository salonRepository;
    private final EmployeeRepository employeeRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;


    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Bu kullanıcı adı zaten alınmış: " + request.getUsername());
        }
       // $2a$10$v4DsRSTLDsT0IPbd07SbEeW8lR.FHkpiIX4p1MbcS3YSPZoYEPLsG
        //$2a$10$Czb75uzfpR.P.Z9c1GKaPeR4hdVpjINr72IoacJD5GNbA1/Gvm0V2
        User user = new User();
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        assignScope(user, request);
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
                .orElseThrow(() -> new BadRequestException("Geçersiz yenileme belirteci."));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new BadRequestException("Yenileme belirtecinin süresi dolmuş; lütfen tekrar giriş yapın.");
        }

        return buildAuthResponse(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequestDto request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mevcut şifre hatalı.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getMe(String username) {
        return new UserResponseDto(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + username)));
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

        UserResponseDto userResponse = new UserResponseDto(user);
        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .role(user.getRole())
                .salonId(userResponse.getSalonId())
                .salonIds(userResponse.getSalonIds())
                .salons(userResponse.getSalons())
                .employeeId(user.getEmployee() != null ? user.getEmployee().getId() : null)
                .expiresAt(expiresAt)
                .build();
    }

    private void assignScope(User user, RegisterRequestDto request) {
        if (request.getRole() == null) {
            throw new BadRequestException("Rol alanı zorunludur.");
        }
        if (request.getRole() == Role.SALON_OWNER) {
            if (request.getEmployeeId() != null) {
                throw new BadRequestException("SALON_OWNER için employeeId kullanılmamalıdır.");
            }
            Set<Long> salonIds = new LinkedHashSet<>();
            if (request.getSalonIds() != null) {
                salonIds.addAll(request.getSalonIds());
            }
            if (request.getSalonId() != null) {
                salonIds.add(request.getSalonId());
            }
            Set<Salon> salons = new LinkedHashSet<>();
            salonIds.forEach(id -> salons.add(findSalon(id)));
            user.setAuthorizedSalons(salons);

            Long defaultSalonId = request.getDefaultSalonId() != null
                    ? request.getDefaultSalonId()
                    : request.getSalonId();
            if (defaultSalonId == null && salonIds.size() == 1) {
                defaultSalonId = salonIds.iterator().next();
            }
            if (defaultSalonId != null && !salonIds.contains(defaultSalonId)) {
                throw new BadRequestException("defaultSalonId, salonIds içinde bulunmalıdır.");
            }
            if (defaultSalonId != null) {
                user.setSalon(findSalon(defaultSalonId));
            }
            return;
        }
        if (request.getRole() == Role.EMPLOYEE) {
            if (request.getEmployeeId() == null) {
                throw new BadRequestException("EMPLOYEE için employeeId zorunludur.");
            }
            if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
                throw new DuplicateResourceException("Bu çalışan için zaten bir kullanıcı hesabı bulunuyor.");
            }
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Çalışan", request.getEmployeeId()));
            if (Boolean.FALSE.equals(employee.getActive()) || Boolean.FALSE.equals(employee.getSalon().getActive())) {
                throw new BadRequestException("Pasif çalışan veya salon için kullanıcı oluşturulamaz.");
            }
            if (request.getSalonId() != null && !request.getSalonId().equals(employee.getSalon().getId())) {
                throw new BadRequestException("employeeId ile salonId aynı salona ait olmalıdır.");
            }
            user.setEmployee(employee);
            user.setSalon(employee.getSalon());
            user.getAuthorizedSalons().add(employee.getSalon());
            return;
        }
        if (request.getSalonId() != null || request.getSalonIds() != null
                || request.getDefaultSalonId() != null || request.getEmployeeId() != null) {
            throw new BadRequestException(
                    "ADMIN ve CUSTOMER rolleri için salonId/salonIds/defaultSalonId/employeeId kullanılmamalıdır.");
        }
    }

    private Salon findSalon(Long id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", id));
        if (Boolean.FALSE.equals(salon.getActive())) {
            throw new ResourceNotFoundException("Salon", id);
        }
        return salon;
    }
}
