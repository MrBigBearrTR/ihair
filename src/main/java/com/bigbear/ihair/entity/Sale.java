package com.bigbear.ihair.entity;

import com.bigbear.ihair.common.BaseEntity;
import com.bigbear.ihair.entity.enums.DiscountType;
import com.bigbear.ihair.entity.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "sales", uniqueConstraints = @UniqueConstraint(
        name = "uq_sales_source_appointment", columnNames = "source_appointment_id"))
public class Sale extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_appointment_id", unique = true)
    private Appointment sourceAppointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status = SaleStatus.OPEN;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;
    private String campaignCodeSnapshot;
    private String campaignNameSnapshot;
    @Enumerated(EnumType.STRING)
    private DiscountType campaignDiscountTypeSnapshot;
    @Column(precision = 12, scale = 2)
    private BigDecimal campaignDiscountValueSnapshot;
    @Column(precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private LocalDateTime campaignAppliedAt;

    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SalePayment> payments = new ArrayList<>();

    @Version
    private Long version;

    public void addItem(SaleItem item) {
        item.setSale(this);
        items.add(item);
    }

    public void addPayment(SalePayment payment) {
        payment.setSale(this);
        payments.add(payment);
    }

}
