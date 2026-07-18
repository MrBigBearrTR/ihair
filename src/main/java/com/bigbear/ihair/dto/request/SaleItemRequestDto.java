package com.bigbear.ihair.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleItemRequestDto {
    private Long serviceId;
    private Long employeeId;
    private Integer quantity;
    private Integer position;
}
