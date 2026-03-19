package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.Campaign;
import com.bigbear.ihair.entity.enums.DiscountType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CampaignResponseDto {
    private final Long id;
    private final String name;
    private final String description;
    private final String code;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final Integer maxUsageCount;
    private final Integer usedCount;
    private final Boolean isCustomerSpecific;
    private final Long customerId;
    private final String customerName;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private final Boolean active;
    private final LocalDateTime createdAt;

    public CampaignResponseDto(Campaign campaign) {
        this.id = campaign.getId();
        this.name = campaign.getName();
        this.description = campaign.getDescription();
        this.code = campaign.getCode();
        this.discountType = campaign.getDiscountType();
        this.discountValue = campaign.getDiscountValue();
        this.maxUsageCount = campaign.getMaxUsageCount();
        this.usedCount = campaign.getUsedCount();
        this.isCustomerSpecific = campaign.getIsCustomerSpecific();
        this.customerId = campaign.getCustomer() != null ? campaign.getCustomer().getId() : null;
        this.customerName = campaign.getCustomer() != null
                ? campaign.getCustomer().getFirstName() + " " + campaign.getCustomer().getLastName()
                : null;
        this.validFrom = campaign.getValidFrom();
        this.validTo = campaign.getValidTo();
        this.active = campaign.getActive();
        this.createdAt = campaign.getCreatedAt();
    }
}
