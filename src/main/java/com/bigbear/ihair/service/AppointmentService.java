package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.AppointmentRequestDto;
import com.bigbear.ihair.dto.request.AppointmentStatusRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;
import com.bigbear.ihair.dto.response.AppointmentWeekResponseDto;
import com.bigbear.ihair.dto.response.PagedResponseDto;
import com.bigbear.ihair.entity.enums.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    List<AppointmentResponseDto> getAll(Long salonId);
    PagedResponseDto<AppointmentResponseDto> getPaged(
            Long salonId, AppointmentStatus status, Boolean active,
            LocalDate from, LocalDate to, int page, int size);
    AppointmentWeekResponseDto getWeek(Long salonId, LocalDate weekStart);
    AppointmentResponseDto getById(Long id);
    AppointmentResponseDto create(AppointmentRequestDto request);
    AppointmentResponseDto update(Long id, AppointmentRequestDto request);
    AppointmentResponseDto updateStatus(Long id, AppointmentStatusRequestDto request);
    void delete(Long id);
}
