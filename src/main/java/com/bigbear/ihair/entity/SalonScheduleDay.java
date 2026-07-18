package com.bigbear.ihair.entity;

import com.bigbear.ihair.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "salon_schedule_days", uniqueConstraints = @UniqueConstraint(
        name = "uq_salon_schedule_day", columnNames = {"schedule_id", "day_of_week"}))
public class SalonScheduleDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private SalonSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private Boolean closed = false;

    private LocalTime opensAt;
    private LocalTime closesAt;
}
