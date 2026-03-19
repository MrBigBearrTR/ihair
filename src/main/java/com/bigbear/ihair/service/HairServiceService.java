package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.HairServiceRequestDto;
import com.bigbear.ihair.dto.response.HairServiceResponseDto;

import java.util.List;

public interface HairServiceService {
    List<HairServiceResponseDto> getAll(Long salonId);
    HairServiceResponseDto getById(Long id);
    HairServiceResponseDto create(HairServiceRequestDto request);
    HairServiceResponseDto update(Long id, HairServiceRequestDto request);
    void delete(Long id);
}
