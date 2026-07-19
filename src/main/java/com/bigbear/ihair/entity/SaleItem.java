package com.bigbear.ihair.entity;

import com.bigbear.ihair.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "sale_items", uniqueConstraints = @UniqueConstraint(
        name = "uq_sale_items_position", columnNames = {"sale_id", "position"}))
public class SaleItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hair_service_id", nullable = false)
    private HairService service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal listPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountShare = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal netLineTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private String serviceNameSnapshot;

    @Column(nullable = false)
    private String employeeNameSnapshot;
}
