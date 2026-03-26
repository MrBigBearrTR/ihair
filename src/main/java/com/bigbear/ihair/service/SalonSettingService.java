package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.SalonSettingRequestDto;
import com.bigbear.ihair.dto.response.SalonSettingResponseDto;

import java.util.List;

public interface SalonSettingService {

    List<SalonSettingResponseDto> getAllBySalon(Long salonId);

    SalonSettingResponseDto getByKey(Long salonId, String key);

    SalonSettingResponseDto upsert(Long salonId, String key, SalonSettingRequestDto request);

    void delete(Long salonId, String key);
}
