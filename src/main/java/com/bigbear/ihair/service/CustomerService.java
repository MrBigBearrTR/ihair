package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.CustomerRequestDto;
import com.bigbear.ihair.dto.response.CustomerResponseDto;

import java.util.List;

public interface CustomerService {
    List<CustomerResponseDto> getAll();
    CustomerResponseDto getById(Long id);
    CustomerResponseDto create(CustomerRequestDto request);
    CustomerResponseDto update(Long id, CustomerRequestDto request);
    void delete(Long id);
}
