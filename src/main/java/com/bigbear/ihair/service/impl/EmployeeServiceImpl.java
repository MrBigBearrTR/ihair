package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.EmployeeRequestDto;
import com.bigbear.ihair.dto.response.EmployeeResponseDto;
import com.bigbear.ihair.entity.Employee;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.EmployeeRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.repository.UserRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SalonRepository salonRepository;
    private final SalonAccessService salonAccessService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getAll(Long salonId) {
        Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
        List<Employee> employees = salonIds == null
                ? employeeRepository.findAllByActiveTrue()
                : employeeRepository.findAllBySalonIdInAndActiveTrue(salonIds);
        return employees.stream().map(EmployeeResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getById(Long id) {
        Employee employee = findActiveById(id);
        salonAccessService.requireSalonAccess(employee.getSalon().getId());
        return new EmployeeResponseDto(employee);
    }

    @Override
    @Transactional
    public EmployeeResponseDto create(EmployeeRequestDto request) {
        Salon salon = findActiveSalon(salonAccessService.resolveSalonId(request.getSalonId()));
        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setSalon(salon);
        return new EmployeeResponseDto(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public EmployeeResponseDto update(Long id, EmployeeRequestDto request) {
        Employee employee = findActiveById(id);
        salonAccessService.requireSalonAccess(employee.getSalon().getId());
        Salon salon = request.getSalonId() == null
                ? employee.getSalon()
                : findActiveSalon(salonAccessService.resolveSalonId(request.getSalonId()));
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setSalon(salon);
        Employee saved = employeeRepository.save(employee);
        userRepository.findByEmployeeId(saved.getId()).ifPresent(user -> {
            user.setSalon(salon);
            user.getAuthorizedSalons().clear();
            user.getAuthorizedSalons().add(salon);
            userRepository.save(user);
        });
        return new EmployeeResponseDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Employee employee = findActiveById(id);
        salonAccessService.requireSalonAccess(employee.getSalon().getId());
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private Employee findActiveById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Çalışan", id));
        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new ResourceNotFoundException("Çalışan", id);
        }
        return employee;
    }

    private Salon findActiveSalon(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", salonId));
        if (Boolean.FALSE.equals(salon.getActive())) {
            throw new ResourceNotFoundException("Salon", salonId);
        }
        return salon;
    }
}
