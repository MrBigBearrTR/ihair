package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.AppointmentRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;

import java.util.List;

public interface AppointmentService {
    List<AppointmentResponseDto> getAll();
    AppointmentResponseDto getById(Long id);
    AppointmentResponseDto create(AppointmentRequestDto request);
    AppointmentResponseDto update(Long id, AppointmentRequestDto request);
    void delete(Long id);
}
