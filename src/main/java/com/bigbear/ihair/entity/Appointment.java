package com.bigbear.ihair.entity;

import com.bigbear.ihair.common.BaseEntity;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hair_service_id", nullable = false)
    private HairService hairService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Column(nullable = false)
    private LocalDateTime appointmentDateTime;

    private Integer durationMinutesSnapshot;

    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean scheduleOverridden = false;

    private String scheduleOverrideReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_override_by")
    private User scheduleOverrideBy;

    private LocalDateTime scheduleOverrideAt;

    @Version
    private Long version;

    @OneToOne(mappedBy = "sourceAppointment", fetch = FetchType.LAZY)
    private Sale sale;
}
