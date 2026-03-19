package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.response.CustomerResponseDto;
import com.bigbear.ihair.repository.CustomerRepository;
import com.bigbear.ihair.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getActiveCustomers() {
        return customerRepository.findAllByActiveTrue()
                .stream()
                .map(CustomerResponseDto::new)
                .toList();
    }
}
