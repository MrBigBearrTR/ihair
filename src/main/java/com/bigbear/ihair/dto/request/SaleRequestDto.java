package com.bigbear.ihair.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SaleRequestDto {
    private Long salonId;
    private Long customerId;
    private Long sourceAppointmentId;
    private String notes;
    private List<SaleItemRequestDto> items = new ArrayList<>();
}
