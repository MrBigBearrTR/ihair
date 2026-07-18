package com.bigbear.ihair.dto.request;

import com.bigbear.ihair.entity.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SalePaymentRequestDto {
    private PaymentMethod method;
    private BigDecimal amount;
}
