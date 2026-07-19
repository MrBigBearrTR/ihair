package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Sale;
import com.bigbear.ihair.entity.enums.DiscountType;
import com.bigbear.ihair.entity.enums.SaleStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class SaleListResponseDto {
    private final Long id;
    private final Long salonId;
    private final Long customerId;
    private final String customerName;
    private final Long sourceAppointmentId;
    private final SaleStatus status;
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final Long campaignId;
    private final String campaignCode;
    private final String campaignName;
    private final DiscountType campaignDiscountType;
    private final BigDecimal campaignDiscountValue;
    private final String notes;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;

    public SaleListResponseDto(Sale sale) {
        id = sale.getId();
        salonId = sale.getSalon().getId();
        customerId = sale.getCustomer().getId();
        customerName = (sale.getCustomer().getFirstName() + " "
                + sale.getCustomer().getLastName()).trim();
        sourceAppointmentId =
                sale.getSourceAppointment() == null ? null : sale.getSourceAppointment().getId();
        status = sale.getStatus();
        subtotal = sale.getSubtotal();
        discountAmount = sale.getDiscountAmount();
        totalAmount = sale.getTotalAmount();
        campaignId = sale.getCampaign() == null ? null : sale.getCampaign().getId();
        campaignCode = sale.getCampaignCodeSnapshot();
        campaignName = sale.getCampaignNameSnapshot();
        campaignDiscountType = sale.getCampaignDiscountTypeSnapshot();
        campaignDiscountValue = sale.getCampaignDiscountValueSnapshot();
        notes = sale.getNotes();
        createdAt = sale.getCreatedAt();
        completedAt = sale.getCompletedAt();
    }
}
