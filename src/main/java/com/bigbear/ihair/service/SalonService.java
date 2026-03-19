package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.SalonRequestDto;
import com.bigbear.ihair.dto.response.SalonResponseDto;

import java.util.List;

public interface SalonService {
    List<SalonResponseDto> getAll();
    SalonResponseDto getById(Long id);
    SalonResponseDto create(SalonRequestDto request);
    SalonResponseDto update(Long id, SalonRequestDto request);
    void delete(Long id);
}
