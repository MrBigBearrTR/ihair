package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.CampaignRequestDto;
import com.bigbear.ihair.dto.response.CampaignResponseDto;

import java.util.List;

public interface CampaignService {
    List<CampaignResponseDto> getAll(Long salonId);
    CampaignResponseDto getById(Long id);
    CampaignResponseDto create(CampaignRequestDto request);
    CampaignResponseDto update(Long id, CampaignRequestDto request);
    void delete(Long id);
    CampaignResponseDto validate(String code);
}
