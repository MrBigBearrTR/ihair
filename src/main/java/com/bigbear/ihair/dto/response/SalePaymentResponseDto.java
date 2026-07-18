package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.SalePayment;
import com.bigbear.ihair.entity.enums.PaymentMethod;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class SalePaymentResponseDto {
    private final Long id;
    private final PaymentMethod method;
    private final BigDecimal amount;

    public SalePaymentResponseDto(SalePayment payment) {
        this.id = payment.getId();
        this.method = payment.getMethod();
        this.amount = payment.getAmount();
    }
}
