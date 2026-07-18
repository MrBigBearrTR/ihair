package com.bigbear.ihair.security;

import com.bigbear.ihair.entity.Employee;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.SalonScopeException;
import com.bigbear.ihair.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SalonAccessServiceTest {

    private UserRepository userRepository;
    private SalonAccessService accessService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        accessService = new SalonAccessService(userRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", "password", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerWithZeroSalonsGetsStableScopeError() {
        User owner = user(Role.SALON_OWNER);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        SalonScopeException error = assertThrows(
                SalonScopeException.class, () -> accessService.resolveSalonId(null));

        assertEquals("SALON_ACCESS_MISSING", error.getCode());
        SalonScopeException listError = assertThrows(
                SalonScopeException.class, () -> accessService.resolveSalonIdsForList(null));
        assertEquals("SALON_ACCESS_MISSING", listError.getCode());
    }

    @Test
    void ownerWithOneSalonIsAutomaticallyScoped() {
        User owner = ownerWithSalons(10L);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        assertEquals(10L, accessService.resolveSalonId(null));
        assertEquals(Set.of(10L), accessService.resolveSalonIdsForList(null));
    }

    @Test
    void ownerWithMultipleSalonsMustSelectForCreateButListsAll() {
        User owner = ownerWithSalons(10L, 20L);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        SalonScopeException error = assertThrows(
                SalonScopeException.class, () -> accessService.resolveSalonId(null));

        assertEquals("SALON_SCOPE_AMBIGUOUS", error.getCode());
        assertEquals(Set.of(10L, 20L), accessService.resolveSalonIdsForList(null));
        assertEquals(20L, accessService.resolveSalonId(20L));
        assertThrows(AccessDeniedException.class, () -> accessService.resolveSalonId(30L));
    }

    @Test
    void employeeScopeComesFromEmployeeSalon() {
        User employeeUser = user(Role.EMPLOYEE);
        Salon employeeSalon = salon(30L);
        Employee employee = new Employee();
        employee.setSalon(employeeSalon);
        employeeUser.setEmployee(employee);
        employeeUser.setSalon(salon(99L));
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(employeeUser));

        assertEquals(30L, accessService.resolveSalonId(null));
        assertEquals(Set.of(30L), accessService.resolveSalonIdsForList(null));
    }

    private User ownerWithSalons(Long... ids) {
        User owner = user(Role.SALON_OWNER);
        Set<Salon> salons = new LinkedHashSet<>();
        for (Long id : ids) {
            salons.add(salon(id));
        }
        owner.setAuthorizedSalons(salons);
        return owner;
    }

    private User user(Role role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    private Salon salon(Long id) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setActive(true);
        return salon;
    }
}
