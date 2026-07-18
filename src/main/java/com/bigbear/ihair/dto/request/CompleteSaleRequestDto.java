package com.bigbear.ihair.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CompleteSaleRequestDto {
    private List<SalePaymentRequestDto> payments = new ArrayList<>();
}
