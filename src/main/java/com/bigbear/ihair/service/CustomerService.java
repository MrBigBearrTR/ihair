package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.response.CustomerResponseDto;

import java.util.List;

public interface CustomerService {

    List<CustomerResponseDto> getActiveCustomers();
}
