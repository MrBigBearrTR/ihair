package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.SaleItem;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class SaleQuoteItemResponseDto {
    private final Long serviceId;
    private final Long employeeId;
    private final Integer quantity;
    private final Integer position;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;
    private final BigDecimal discountShare;
    private final BigDecimal netLineTotal;
    private final String serviceName;
    private final String employeeName;

    public SaleQuoteItemResponseDto(SaleItem item) {
        serviceId = item.getService().getId();
        employeeId = item.getEmployee().getId();
        quantity = item.getQuantity();
        position = item.getPosition();
        unitPrice = item.getUnitPrice();
        lineTotal = item.getLineTotal();
        discountShare = item.getDiscountShare();
        netLineTotal = item.getNetLineTotal();
        serviceName = item.getServiceNameSnapshot();
        employeeName = item.getEmployeeNameSnapshot();
    }
}
