package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.enums.Role;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class UserResponseDto {
    private final Long id;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final Role role;
    private final Boolean active;
    private final Long salonId;
    private final List<Long> salonIds;
    private final List<SalonResponseDto> salons;
    private final Long employeeId;
    private final LocalDateTime createdAt;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.role = user.getRole();
        this.active = user.getActive();
        Salon defaultSalon = user.getRole() == Role.EMPLOYEE && user.getEmployee() != null
                ? user.getEmployee().getSalon()
                : user.getSalon();
        this.salonId = defaultSalon != null ? defaultSalon.getId() : null;
        Set<Salon> authorized = new LinkedHashSet<>(user.getAuthorizedSalons());
        if (defaultSalon != null) {
            authorized.add(defaultSalon);
        }
        this.salonIds = authorized.stream().map(Salon::getId).toList();
        this.salons = authorized.stream().map(SalonResponseDto::new).toList();
        this.employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;
        this.createdAt = user.getCreatedAt();
    }
}
