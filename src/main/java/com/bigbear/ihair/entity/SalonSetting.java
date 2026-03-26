package com.bigbear.ihair.entity;

import com.bigbear.ihair.common.BaseEntity;
import com.bigbear.ihair.entity.enums.SettingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "salon_settings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_salon_settings_salon_key",
        columnNames = {"salon_id", "setting_key"}
    )
)
public class SalonSetting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_type", nullable = false, length = 20)
    private SettingType settingType;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(length = 255)
    private String description;
}
