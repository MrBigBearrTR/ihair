package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.UserSalonAssignmentRequestDto;
import com.bigbear.ihair.dto.response.UserResponseDto;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final SalonRepository salonRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserResponseDto>> getAll() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(UserResponseDto::new).toList());
    }

    @PutMapping("/{id}/salons")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<UserResponseDto> assignSalons(
            @PathVariable Long id,
            @RequestBody UserSalonAssignmentRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        if (user.getRole() != Role.SALON_OWNER) {
            throw new BadRequestException("Salon ataması yalnız SALON_OWNER kullanıcıları için yapılabilir.");
        }

        Set<Long> salonIds = request.getSalonIds() == null
                ? Set.of()
                : new LinkedHashSet<>(request.getSalonIds());
        if (request.getDefaultSalonId() != null && !salonIds.contains(request.getDefaultSalonId())) {
            throw new BadRequestException("defaultSalonId, salonIds içinde bulunmalıdır.");
        }

        Set<Salon> salons = new LinkedHashSet<>();
        for (Long salonId : salonIds) {
            Salon salon = salonRepository.findById(salonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Salon", salonId));
            if (Boolean.FALSE.equals(salon.getActive())) {
                throw new BadRequestException("Pasif salon kullanıcıya atanamaz: " + salonId);
            }
            salons.add(salon);
        }
        user.setAuthorizedSalons(salons);
        Long defaultSalonId = request.getDefaultSalonId();
        if (defaultSalonId == null && salons.size() == 1) {
            defaultSalonId = salons.iterator().next().getId();
        }
        Long finalDefaultSalonId = defaultSalonId;
        user.setSalon(finalDefaultSalonId == null ? null : salons.stream()
                .filter(salon -> salon.getId().equals(finalDefaultSalonId))
                .findFirst()
                .orElseThrow());
        return ResponseEntity.ok(new UserResponseDto(userRepository.save(user)));
    }
}
