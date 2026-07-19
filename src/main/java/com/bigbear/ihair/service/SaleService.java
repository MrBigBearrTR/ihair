package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.CompleteSaleRequestDto;
import com.bigbear.ihair.dto.request.SaleRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;
import com.bigbear.ihair.dto.response.PagedResponseDto;
import com.bigbear.ihair.dto.response.SaleListResponseDto;
import com.bigbear.ihair.dto.response.SaleResponseDto;
import com.bigbear.ihair.dto.response.SaleQuoteResponseDto;
import com.bigbear.ihair.entity.enums.SaleStatus;

import java.time.LocalDate;
import java.util.List;

public interface SaleService {
    List<SaleResponseDto> getAll(Long salonId, SaleStatus status);
    List<SaleResponseDto> getAll(
            Long salonId, SaleStatus status, LocalDate from, LocalDate to, Long employeeId);
    PagedResponseDto<SaleListResponseDto> getPaged(
            Long salonId, SaleStatus status, LocalDate from, LocalDate to,
            Long employeeId, int page, int size);
    SaleResponseDto getById(Long id);
    SaleQuoteResponseDto quote(SaleRequestDto request);
    SaleResponseDto create(SaleRequestDto request);
    SaleResponseDto update(Long id, SaleRequestDto request);
    SaleResponseDto complete(Long id, CompleteSaleRequestDto request);
    SaleResponseDto cancel(Long id);
    List<AppointmentResponseDto> getAvailableAppointments(Long salonId);
}
