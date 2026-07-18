package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.UserSalonAssignmentRequestDto;
import com.bigbear.ihair.dto.response.UserResponseDto;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Test
    void assignsMultipleSalonsAndValidatedDefaultToOwner() {
        UserRepository users = mock(UserRepository.class);
        SalonRepository salons = mock(SalonRepository.class);
        User owner = user(Role.SALON_OWNER);
        Salon first = salon(1L);
        Salon second = salon(2L);
        UserSalonAssignmentRequestDto request = mock(UserSalonAssignmentRequestDto.class);
        when(request.getSalonIds()).thenReturn(Set.of(1L, 2L));
        when(request.getDefaultSalonId()).thenReturn(2L);
        when(users.findById(7L)).thenReturn(Optional.of(owner));
        when(users.save(owner)).thenReturn(owner);
        when(salons.findById(1L)).thenReturn(Optional.of(first));
        when(salons.findById(2L)).thenReturn(Optional.of(second));

        ResponseEntity<UserResponseDto> response =
                new UserController(users, salons).assignSalons(7L, request);

        assertEquals(2L, response.getBody().getSalonId());
        assertEquals(Set.of(1L, 2L), Set.copyOf(response.getBody().getSalonIds()));
    }

    @Test
    void rejectsDefaultSalonOutsideAssignmentAndNonOwnerAssignment() {
        UserRepository users = mock(UserRepository.class);
        SalonRepository salons = mock(SalonRepository.class);
        UserSalonAssignmentRequestDto request = mock(UserSalonAssignmentRequestDto.class);
        when(request.getSalonIds()).thenReturn(Set.of(1L));
        when(request.getDefaultSalonId()).thenReturn(2L);
        when(users.findById(7L)).thenReturn(Optional.of(user(Role.SALON_OWNER)));

        UserController controller = new UserController(users, salons);
        assertThrows(BadRequestException.class, () -> controller.assignSalons(7L, request));

        when(users.findById(7L)).thenReturn(Optional.of(user(Role.EMPLOYEE)));
        assertThrows(BadRequestException.class, () -> controller.assignSalons(7L, request));
    }

    private User user(Role role) {
        User user = new User();
        user.setRole(role);
        user.setUsername("user");
        return user;
    }

    private Salon salon(Long id) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setName("Salon " + id);
        salon.setActive(true);
        return salon;
    }
}
