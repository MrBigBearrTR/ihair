package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Customer;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CustomerResponseDto {
    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String email;
    private final Boolean active;
    private final String notes;
    private final Long salonId;
    private final LocalDateTime createdAt;

    public CustomerResponseDto(Customer customer) {
        this.id = customer.getId();
        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.phone = customer.getPhone();
        this.email = customer.getEmail();
        this.active = customer.getActive();
        this.notes = customer.getNotes();
        this.salonId = customer.getSalon() != null ? customer.getSalon().getId() : null;
        this.createdAt = customer.getCreatedAt();
    }
}
