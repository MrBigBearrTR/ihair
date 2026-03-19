package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.AppointmentRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;
import com.bigbear.ihair.entity.Appointment;
import com.bigbear.ihair.entity.Campaign;
import com.bigbear.ihair.entity.Customer;
import com.bigbear.ihair.entity.Employee;
import com.bigbear.ihair.entity.HairService;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import com.bigbear.ihair.entity.enums.DiscountType;
import com.bigbear.ihair.exception.AppointmentConflictException;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.AppointmentRepository;
import com.bigbear.ihair.repository.CampaignRepository;
import com.bigbear.ihair.repository.CustomerRepository;
import com.bigbear.ihair.repository.EmployeeRepository;
import com.bigbear.ihair.repository.HairServiceRepository;
import com.bigbear.ihair.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final HairServiceRepository hairServiceRepository;
    private final CampaignRepository campaignRepository;

    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED
    );

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAll() {
        return appointmentRepository.findAllByStatusNot(AppointmentStatus.CANCELLED)
                .stream().map(AppointmentResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDto getById(Long id) {
        return new AppointmentResponseDto(findById(id));
    }

    @Override
    @Transactional
    public AppointmentResponseDto create(AppointmentRequestDto request) {
        Customer customer = findActiveCustomer(request.getCustomerId());
        Employee employee = findActiveEmployee(request.getEmployeeId());
        HairService hairService = findActiveHairService(request.getHairServiceId());

        checkConflict(request.getEmployeeId(), request.getAppointmentDateTime(), null);

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setEmployee(employee);
        appointment.setHairService(hairService);
        appointment.setAppointmentDateTime(request.getAppointmentDateTime());
        appointment.setNotes(request.getNotes());
        appointment.setStatus(request.getStatus() != null ? request.getStatus() : AppointmentStatus.PENDING);

        applyDiscount(appointment, hairService.getPrice(), request.getCampaignCode());

        return new AppointmentResponseDto(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponseDto update(Long id, AppointmentRequestDto request) {
        Appointment appointment = findById(id);
        Customer customer = findActiveCustomer(request.getCustomerId());
        Employee employee = findActiveEmployee(request.getEmployeeId());
        HairService hairService = findActiveHairService(request.getHairServiceId());

        if (!appointment.getEmployee().getId().equals(request.getEmployeeId())
                || !appointment.getAppointmentDateTime().equals(request.getAppointmentDateTime())) {
            checkConflict(request.getEmployeeId(), request.getAppointmentDateTime(), id);
        }

        appointment.setCustomer(customer);
        appointment.setEmployee(employee);
        appointment.setHairService(hairService);
        appointment.setAppointmentDateTime(request.getAppointmentDateTime());
        appointment.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }

        applyDiscount(appointment, hairService.getPrice(), request.getCampaignCode());

        return new AppointmentResponseDto(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Appointment appointment = findById(id);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    private void checkConflict(Long employeeId, LocalDateTime dateTime, Long excludeId) {
        boolean conflict = excludeId == null
                ? appointmentRepository.existsByEmployeeIdAndAppointmentDateTimeAndStatusIn(employeeId, dateTime, ACTIVE_STATUSES)
                : appointmentRepository.existsByEmployeeIdAndAppointmentDateTimeAndStatusInAndIdNot(employeeId, dateTime, ACTIVE_STATUSES, excludeId);
        if (conflict) {
            throw new AppointmentConflictException(
                    "Bu çalışan için " + dateTime + " saatinde zaten aktif bir randevu mevcut.");
        }
    }

    private void applyDiscount(Appointment appointment, BigDecimal basePrice, String campaignCode) {
        if (campaignCode == null || campaignCode.isBlank()) {
            appointment.setFinalPrice(basePrice);
            return;
        }

        Campaign campaign = campaignRepository.findByCode(campaignCode)
                .orElseThrow(() -> new BadRequestException("Geçersiz kampanya kodu: " + campaignCode));

        if (Boolean.FALSE.equals(campaign.getActive())) {
            throw new BadRequestException("Bu kampanya artık aktif değil.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (campaign.getValidFrom() != null && now.isBefore(campaign.getValidFrom())) {
            throw new BadRequestException("Kampanya henüz başlamamış.");
        }
        if (campaign.getValidTo() != null && now.isAfter(campaign.getValidTo())) {
            throw new BadRequestException("Kampanyanın geçerlilik süresi dolmuş.");
        }
        if (campaign.getMaxUsageCount() != null && campaign.getUsedCount() >= campaign.getMaxUsageCount()) {
            throw new BadRequestException("Kampanya kullanım limiti dolmuş.");
        }

        BigDecimal finalPrice;
        if (campaign.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal discount = basePrice.multiply(campaign.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            finalPrice = basePrice.subtract(discount).max(BigDecimal.ZERO);
        } else if (campaign.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            finalPrice = basePrice.subtract(campaign.getDiscountValue()).max(BigDecimal.ZERO);
        } else {
            finalPrice = BigDecimal.ZERO;
        }

        campaign.setUsedCount(campaign.getUsedCount() + 1);
        campaignRepository.save(campaign);

        appointment.setCampaign(campaign);
        appointment.setFinalPrice(finalPrice);
    }

    private Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }

    private Customer findActiveCustomer(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        if (Boolean.FALSE.equals(c.getActive())) throw new ResourceNotFoundException("Customer", id);
        return c;
    }

    private Employee findActiveEmployee(Long id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        if (Boolean.FALSE.equals(e.getActive())) throw new ResourceNotFoundException("Employee", id);
        return e;
    }

    private HairService findActiveHairService(Long id) {
        HairService s = hairServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HairService", id));
        if (Boolean.FALSE.equals(s.getActive())) throw new ResourceNotFoundException("HairService", id);
        return s;
    }
}
