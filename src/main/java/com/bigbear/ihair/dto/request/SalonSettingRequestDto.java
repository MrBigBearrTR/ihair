package com.bigbear.ihair.dto.request;

import com.bigbear.ihair.entity.enums.SettingType;
import lombok.Getter;

@Getter
public class SalonSettingRequestDto {
    private SettingType settingType;
    private String settingValue;
    private String description;
}
