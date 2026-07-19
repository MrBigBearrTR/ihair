package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Sale;
import com.bigbear.ihair.entity.enums.DiscountType;
import com.bigbear.ihair.entity.enums.SaleStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class SaleResponseDto {
    private final Long id;
    private final Long salonId;
    private final String salonName;
    private final Long customerId;
    private final String customerName;
    private final Long sourceAppointmentId;
    private final Long createdById;
    private final String createdByUsername;
    private final SaleStatus status;
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final Long campaignId;
    private final String campaignCode;
    private final String campaignName;
    private final DiscountType campaignDiscountType;
    private final BigDecimal campaignDiscountValue;
    private final LocalDateTime campaignAppliedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime cancelledAt;
    private final String notes;
    private final List<SaleItemResponseDto> items;
    private final List<SalePaymentResponseDto> payments;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long version;

    public SaleResponseDto(Sale sale) {
        this.id = sale.getId();
        this.salonId = sale.getSalon().getId();
        this.salonName = sale.getSalon().getName();
        this.customerId = sale.getCustomer().getId();
        this.customerName = fullName(sale.getCustomer().getFirstName(), sale.getCustomer().getLastName());
        this.sourceAppointmentId = sale.getSourceAppointment() == null ? null : sale.getSourceAppointment().getId();
        this.createdById = sale.getCreatedBy().getId();
        this.createdByUsername = sale.getCreatedBy().getUsername();
        this.status = sale.getStatus();
        this.subtotal = sale.getSubtotal();
        this.discountAmount = sale.getDiscountAmount();
        this.totalAmount = sale.getTotalAmount();
        this.campaignId = sale.getCampaign() == null ? null : sale.getCampaign().getId();
        this.campaignCode = sale.getCampaignCodeSnapshot();
        this.campaignName = sale.getCampaignNameSnapshot();
        this.campaignDiscountType = sale.getCampaignDiscountTypeSnapshot();
        this.campaignDiscountValue = sale.getCampaignDiscountValueSnapshot();
        this.campaignAppliedAt = sale.getCampaignAppliedAt();
        this.completedAt = sale.getCompletedAt();
        this.cancelledAt = sale.getCancelledAt();
        this.notes = sale.getNotes();
        this.items = sale.getItems().stream().map(SaleItemResponseDto::new).toList();
        this.payments = sale.getPayments().stream().map(SalePaymentResponseDto::new).toList();
        this.createdAt = sale.getCreatedAt();
        this.updatedAt = sale.getUpdatedAt();
        this.version = sale.getVersion();
    }

    private static String fullName(String firstName, String lastName) {
        return (firstName + " " + lastName).trim();
    }
}
