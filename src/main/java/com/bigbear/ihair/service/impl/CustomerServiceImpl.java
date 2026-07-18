package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.CustomerRequestDto;
import com.bigbear.ihair.dto.response.CustomerResponseDto;
import com.bigbear.ihair.entity.Customer;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.exception.DuplicateResourceException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.CustomerRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final SalonRepository salonRepository;
    private final SalonAccessService salonAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAll(Long salonId) {
        Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
        List<Customer> customers = salonIds == null
                ? customerRepository.findAllByActiveTrue()
                : customerRepository.findAllBySalonIdInAndActiveTrue(salonIds);
        return customers.stream()
                .map(CustomerResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getById(Long id) {
        Customer customer = findActiveById(id);
        salonAccessService.requireSalonAccess(customer.getSalon() != null ? customer.getSalon().getId() : null);
        return new CustomerResponseDto(customer);
    }

    @Override
    @Transactional
    public CustomerResponseDto create(CustomerRequestDto request) {
        if (request.getPhone() != null && customerRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Bu telefon numarası zaten kullanılıyor: " + request.getPhone());
        }
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Bu e-posta adresi zaten kullanılıyor: " + request.getEmail());
        }
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setNotes(request.getNotes());
        customer.setSalon(findActiveSalon(salonAccessService.resolveSalonId(request.getSalonId())));
        return new CustomerResponseDto(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponseDto update(Long id, CustomerRequestDto request) {
        Customer customer = findActiveById(id);
        salonAccessService.requireSalonAccess(customer.getSalon() != null ? customer.getSalon().getId() : null);
        if (request.getPhone() != null && customerRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
            throw new DuplicateResourceException("Bu telefon numarası zaten kullanılıyor: " + request.getPhone());
        }
        if (request.getEmail() != null && customerRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException("Bu e-posta adresi zaten kullanılıyor: " + request.getEmail());
        }
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setNotes(request.getNotes());
        if (request.getSalonId() != null) {
            customer.setSalon(findActiveSalon(salonAccessService.resolveSalonId(request.getSalonId())));
        }
        return new CustomerResponseDto(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = findActiveById(id);
        salonAccessService.requireSalonAccess(customer.getSalon() != null ? customer.getSalon().getId() : null);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    private Customer findActiveById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Müşteri", id));
        if (Boolean.FALSE.equals(customer.getActive())) {
            throw new ResourceNotFoundException("Müşteri", id);
        }
        return customer;
    }

    private Salon findActiveSalon(Long id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", id));
        if (Boolean.FALSE.equals(salon.getActive())) {
            throw new ResourceNotFoundException("Salon", id);
        }
        return salon;
    }
}
