package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.SaleItem;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class SaleItemResponseDto {
    private final Long id;
    private final Long serviceId;
    private final Long employeeId;
    private final Integer quantity;
    private final Integer position;
    private final BigDecimal unitPrice;
    private final BigDecimal listPrice;
    private final BigDecimal lineTotal;
    private final String serviceName;
    private final String employeeName;

    public SaleItemResponseDto(SaleItem item) {
        this.id = item.getId();
        this.serviceId = item.getService().getId();
        this.employeeId = item.getEmployee().getId();
        this.quantity = item.getQuantity();
        this.position = item.getPosition();
        this.unitPrice = item.getUnitPrice();
        this.listPrice = item.getListPrice();
        this.lineTotal = item.getLineTotal();
        this.serviceName = item.getServiceNameSnapshot();
        this.employeeName = item.getEmployeeNameSnapshot();
    }
}
