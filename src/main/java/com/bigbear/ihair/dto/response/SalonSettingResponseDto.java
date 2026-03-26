package com.bigbear.ihair.dto.response;

import com.bigbear.ihair.entity.SalonSetting;
import com.bigbear.ihair.entity.enums.SettingType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SalonSettingResponseDto {
    private final Long id;
    private final Long salonId;
    private final String salonName;
    private final String settingKey;
    private final SettingType settingType;
    private final String settingValue;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public SalonSettingResponseDto(SalonSetting s) {
        this.id = s.getId();
        this.salonId = s.getSalon().getId();
        this.salonName = s.getSalon().getName();
        this.settingKey = s.getSettingKey();
        this.settingType = s.getSettingType();
        this.settingValue = s.getSettingValue();
        this.description = s.getDescription();
        this.createdAt = s.getCreatedAt();
        this.updatedAt = s.getUpdatedAt();
    }
}
