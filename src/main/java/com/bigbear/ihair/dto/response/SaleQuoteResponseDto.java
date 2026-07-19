package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Sale;
import com.bigbear.ihair.entity.enums.DiscountType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class SaleQuoteResponseDto {
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final Long campaignId;
    private final String campaignCode;
    private final String campaignName;
    private final DiscountType campaignDiscountType;
    private final BigDecimal campaignDiscountValue;
    private final boolean inheritedFromAppointment;
    private final List<SaleQuoteItemResponseDto> items;

    public SaleQuoteResponseDto(Sale sale, boolean inheritedFromAppointment) {
        subtotal = sale.getSubtotal();
        discountAmount = sale.getDiscountAmount();
        totalAmount = sale.getTotalAmount();
        campaignId = sale.getCampaign() == null ? null : sale.getCampaign().getId();
        campaignCode = sale.getCampaignCodeSnapshot();
        campaignName = sale.getCampaignNameSnapshot();
        campaignDiscountType = sale.getCampaignDiscountTypeSnapshot();
        campaignDiscountValue = sale.getCampaignDiscountValueSnapshot();
        this.inheritedFromAppointment = inheritedFromAppointment;
        items = sale.getItems().stream().map(SaleQuoteItemResponseDto::new).toList();
    }
}
