package com.bigbear.ihair.dto.request;

import lombok.Getter;

import java.util.Set;

@Getter
public class UserSalonAssignmentRequestDto {
    private Set<Long> salonIds;
    private Long defaultSalonId;
}
