package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.response.CustomerResponseDto;
import com.bigbear.ihair.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/active")
    public ResponseEntity<List<CustomerResponseDto>> getActiveCustomers() {
        return ResponseEntity.ok(customerService.getActiveCustomers());
    }
}
