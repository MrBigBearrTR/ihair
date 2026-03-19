package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.EmployeeRequestDto;
import com.bigbear.ihair.dto.response.EmployeeResponseDto;

import java.util.List;

public interface EmployeeService {
    List<EmployeeResponseDto> getAll(Long salonId);
    EmployeeResponseDto getById(Long id);
    EmployeeResponseDto create(EmployeeRequestDto request);
    EmployeeResponseDto update(Long id, EmployeeRequestDto request);
    void delete(Long id);
}
