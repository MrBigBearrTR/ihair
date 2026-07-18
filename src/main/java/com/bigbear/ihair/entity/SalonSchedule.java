package com.bigbear.ihair.entity;

import com.bigbear.ihair.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "salon_schedules")
public class SalonSchedule extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false, unique = true)
    private Salon salon;

    @Column(nullable = false)
    private String timeZone = "Europe/Istanbul";

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean configured = false;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC")
    private List<SalonScheduleDay> days = new ArrayList<>();
}
