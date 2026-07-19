package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.AppointmentRequestDto;
import com.bigbear.ihair.dto.request.AppointmentStatusRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;
import com.bigbear.ihair.dto.response.AppointmentWeekResponseDto;
import com.bigbear.ihair.dto.response.PagedResponseDto;
import com.bigbear.ihair.entity.Appointment;
import com.bigbear.ihair.entity.Campaign;
import com.bigbear.ihair.entity.Customer;
import com.bigbear.ihair.entity.Employee;
import com.bigbear.ihair.entity.HairService;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import com.bigbear.ihair.entity.enums.DiscountType;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.AppointmentConflictException;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.exception.OutsideWorkingHoursException;
import com.bigbear.ihair.repository.AppointmentRepository;
import com.bigbear.ihair.repository.CampaignRepository;
import com.bigbear.ihair.repository.CustomerRepository;
import com.bigbear.ihair.repository.EmployeeRepository;
import com.bigbear.ihair.repository.HairServiceRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.AppointmentService;
import com.bigbear.ihair.service.SalonScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private static final int MAX_PAGE_SIZE = 100;

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final HairServiceRepository hairServiceRepository;
    private final CampaignRepository campaignRepository;
    private final SalonAccessService salonAccessService;
    private final SalonScheduleService salonScheduleService;

    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED
    );

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAll(Long salonId) {
        if (salonAccessService.currentRole() == Role.EMPLOYEE) {
            salonAccessService.resolveSalonIdsForList(salonId);
            return appointmentRepository.findAllByEmployeeIdOrderByAppointmentDateTimeDesc(
                            salonAccessService.currentEmployeeId())
                    .stream().map(AppointmentResponseDto::new).toList();
        }
        Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
        List<Appointment> appointments = salonIds == null
                ? appointmentRepository.findAll(Sort.by(Sort.Direction.DESC, "appointmentDateTime"))
                : appointmentRepository.findAllByEmployeeSalonIdInOrderByAppointmentDateTimeDesc(salonIds);
        return appointments
                .stream().map(AppointmentResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AppointmentResponseDto> getPaged(
            Long salonId, AppointmentStatus status, Boolean active,
            LocalDate from, LocalDate to, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("page negatif olamaz; size 1 ile 100 arasında olmalıdır.");
        }
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("to tarihi from tarihinden önce olamaz.");
        }
        Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
        Long employeeId = salonAccessService.currentRole() == Role.EMPLOYEE
                ? salonAccessService.currentEmployeeId() : null;
        List<AppointmentStatus> selectedStatuses = status != null
                ? List.of(status)
                : Boolean.TRUE.equals(active)
                        ? ACTIVE_STATUSES
                        : Boolean.FALSE.equals(active)
                                ? List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED)
                                : List.of(AppointmentStatus.values());
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay();
        Specification<Appointment> specification = (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (salonIds != null) predicates.add(root.get("salon").get("id").in(salonIds));
            if (employeeId != null) predicates.add(criteriaBuilder.equal(
                    root.get("employee").get("id"), employeeId));
            predicates.add(root.get("status").in(selectedStatuses));
            if (fromDateTime != null) predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("appointmentDateTime"), fromDateTime));
            if (toDateTime != null) predicates.add(criteriaBuilder.lessThan(
                    root.get("appointmentDateTime"), toDateTime));
            return criteriaBuilder.and(
                    predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<AppointmentResponseDto> result = appointmentRepository.findAll(
                        specification,
                        PageRequest.of(page, size,
                                Sort.by(Sort.Direction.DESC, "appointmentDateTime")))
                .map(AppointmentResponseDto::new);
        return new PagedResponseDto<>(result);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentWeekResponseDto getWeek(Long salonId, LocalDate weekStart) {
        if (weekStart == null || weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BadRequestException("weekStart bir Pazartesi tarihi olmalıdır.");
        }
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();
        Long resolvedSalonId;
        List<Appointment> appointments;
        if (salonAccessService.currentRole() == Role.EMPLOYEE) {
            resolvedSalonId = salonAccessService.resolveSalonId(salonId);
            appointments = appointmentRepository
                    .findAllByEmployeeIdAndAppointmentDateTimeGreaterThanEqualAndAppointmentDateTimeLessThanAndStatusNotOrderByAppointmentDateTimeAsc(
                            salonAccessService.currentEmployeeId(), start, end, AppointmentStatus.CANCELLED);
        } else {
            if (salonId == null) {
                throw new BadRequestException("salonId alanı zorunludur.");
            }
            resolvedSalonId = salonAccessService.resolveSalonId(salonId);
            appointments = appointmentRepository
                    .findAllByEmployeeSalonIdAndAppointmentDateTimeGreaterThanEqualAndAppointmentDateTimeLessThanAndStatusNotOrderByAppointmentDateTimeAsc(
                            resolvedSalonId, start, end, AppointmentStatus.CANCELLED);
        }
        return new AppointmentWeekResponseDto(
                resolvedSalonId,
                salonScheduleService.getTimeZone(resolvedSalonId),
                weekStart,
                weekStart.plusDays(7),
                appointments.stream().map(AppointmentResponseDto::new).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDto getById(Long id) {
        Appointment appointment = findById(id);
        requireAppointmentAccess(appointment);
        return new AppointmentResponseDto(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponseDto create(AppointmentRequestDto request) {
        Customer customer = findActiveCustomer(request.getCustomerId());
        Employee employee = findActiveEmployeeForUpdate(request.getEmployeeId());
        HairService hairService = findActiveHairService(request.getHairServiceId());

        validateSameSalon(customer, employee, hairService);
        requireEmployeeTargetOwnership(employee);
        salonAccessService.requireSalonAccess(employee.getSalon().getId());
        AppointmentStatus status = request.getStatus() != null ? request.getStatus() : AppointmentStatus.PENDING;
        LocalDateTime start = requireStart(request.getAppointmentDateTime());
        int duration = requirePositiveDuration(hairService.getDurationMinutes());
        LocalDateTime endsAt = start.plusMinutes(duration);
        checkConflict(employee.getId(), start, duration, null, status);
        boolean overridden = validateSchedule(
                employee.getSalon().getId(), start, endsAt, request);

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setEmployee(employee);
        appointment.setHairService(hairService);
        appointment.setSalon(employee.getSalon());
        appointment.setAppointmentDateTime(start);
        appointment.setDurationMinutesSnapshot(duration);
        appointment.setEndsAt(endsAt);
        appointment.setNotes(request.getNotes());
        appointment.setStatus(status);
        applyScheduleOverride(appointment, overridden, request.getOverrideReason());

        applyDiscount(appointment, hairService.getPrice(), request.getCampaignCode(), employee.getSalon().getId(), customer.getId());

        return new AppointmentResponseDto(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponseDto update(Long id, AppointmentRequestDto request) {
        Appointment appointment = findById(id);
        requireAppointmentAccess(appointment);
        requireVersion(appointment, request.getVersion());
        Customer customer = findActiveCustomer(request.getCustomerId());
        Employee employee = findActiveEmployeeForUpdate(request.getEmployeeId());
        HairService hairService = findActiveHairService(request.getHairServiceId());

        validateSameSalon(customer, employee, hairService);
        requireEmployeeTargetOwnership(employee);
        salonAccessService.requireSalonAccess(employee.getSalon().getId());
        AppointmentStatus status = request.getStatus() != null ? request.getStatus() : appointment.getStatus();
        int duration = requirePositiveDuration(hairService.getDurationMinutes());
        LocalDateTime start = requireStart(request.getAppointmentDateTime());
        LocalDateTime endsAt = start.plusMinutes(duration);
        boolean temporalChanged = !Objects.equals(appointment.getEmployee().getId(), employee.getId())
                || !Objects.equals(appointment.getAppointmentDateTime(), start)
                || !Objects.equals(effectiveDuration(appointment), duration);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED && temporalChanged) {
            throw new BadRequestException("Tamamlanmış randevunun zaman veya hizmet süresi değiştirilemez.");
        }
        checkConflict(employee.getId(), start, duration, id, status);
        boolean overridden = false;
        if (temporalChanged) {
            overridden = validateSchedule(employee.getSalon().getId(), start, endsAt, request);
        }

        appointment.setCustomer(customer);
        appointment.setEmployee(employee);
        appointment.setHairService(hairService);
        appointment.setSalon(employee.getSalon());
        appointment.setAppointmentDateTime(start);
        appointment.setDurationMinutesSnapshot(duration);
        appointment.setEndsAt(endsAt);
        appointment.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }
        if (temporalChanged) {
            applyScheduleOverride(appointment, overridden, request.getOverrideReason());
        }

        applyDiscountForUpdate(
                appointment, hairService.getPrice(), request.getCampaignCode(),
                employee.getSalon().getId(), customer.getId());

        return new AppointmentResponseDto(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Appointment appointment = findById(id);
        requireAppointmentAccess(appointment);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponseDto updateStatus(Long id, AppointmentStatusRequestDto request) {
        if (request.getStatus() == null) {
            throw new BadRequestException("status alanı zorunludur.");
        }
        Appointment appointment = findById(id);
        requireAppointmentAccess(appointment);
        findActiveEmployeeForUpdate(appointment.getEmployee().getId());
        checkConflict(
                appointment.getEmployee().getId(),
                appointment.getAppointmentDateTime(),
                effectiveDuration(appointment),
                id,
                request.getStatus());
        appointment.setStatus(request.getStatus());
        return new AppointmentResponseDto(appointmentRepository.save(appointment));
    }

    private void checkConflict(
            Long employeeId,
            LocalDateTime start,
            Integer durationMinutes,
            Long excludeId,
            AppointmentStatus status) {
        if (!ACTIVE_STATUSES.contains(status)) {
            return;
        }
        int safeDurationMinutes = requirePositiveDuration(durationMinutes);
        LocalDateTime end = start.plusMinutes(safeDurationMinutes);
        boolean conflict = appointmentRepository.findAllByEmployeeIdAndStatusIn(employeeId, ACTIVE_STATUSES)
                .stream()
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .anyMatch(existing -> {
                    LocalDateTime existingStart = existing.getAppointmentDateTime();
                    int existingDuration = effectiveDuration(existing);
                    LocalDateTime existingEnd = existing.getEndsAt() != null
                            ? existing.getEndsAt() : existingStart.plusMinutes(existingDuration);
                    return existingStart.isBefore(end) && start.isBefore(existingEnd);
                });
        if (conflict) {
            throw new AppointmentConflictException(
                    "Bu çalışanın belirtilen zaman aralığında aktif bir randevusu bulunuyor.");
        }
    }

    private void applyDiscount(
            Appointment appointment,
            BigDecimal basePrice,
            String campaignCode,
            Long salonId,
            Long customerId) {
        applyDiscount(appointment, basePrice, campaignCode, salonId, customerId, true);
    }

    private void applyDiscountForUpdate(
            Appointment appointment,
            BigDecimal basePrice,
            String campaignCode,
            Long salonId,
            Long customerId) {
        boolean sameCampaign = appointment.getCampaign() != null
                && campaignCode != null
                && appointment.getCampaign().getCode().equals(campaignCode);
        applyDiscount(appointment, basePrice, campaignCode, salonId, customerId, !sameCampaign);
    }

    private void applyDiscount(
            Appointment appointment,
            BigDecimal basePrice,
            String campaignCode,
            Long salonId,
            Long customerId,
            boolean incrementUsage) {
        if (campaignCode == null || campaignCode.isBlank()) {
            appointment.setCampaign(null);
            appointment.setFinalPrice(basePrice);
            return;
        }

        String normalizedCode = campaignCode.trim().toUpperCase(Locale.ROOT);
        Campaign campaign = campaignRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new BadRequestException("Geçersiz kampanya kodu: " + campaignCode));
        if (campaign.getSalon() == null || !salonId.equals(campaign.getSalon().getId())) {
            throw new BadRequestException("Kampanya randevu salonuna ait değildir.");
        }
        if (Boolean.TRUE.equals(campaign.getIsCustomerSpecific())
                && (campaign.getCustomer() == null || !customerId.equals(campaign.getCustomer().getId()))) {
            throw new BadRequestException("Bu kampanya seçilen müşteriye ait değildir.");
        }

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
        if (campaign.getMaxUsageCount() != null
                && campaign.getUsedCount() >= campaign.getMaxUsageCount()) {
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

        appointment.setCampaign(campaign);
        appointment.setFinalPrice(finalPrice);
    }

    private Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Randevu", id));
    }

    private Customer findActiveCustomer(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Müşteri", id));
        if (Boolean.FALSE.equals(c.getActive())) throw new ResourceNotFoundException("Müşteri", id);
        return c;
    }

    private Employee findActiveEmployeeForUpdate(Long id) {
        Employee e = employeeRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Çalışan", id));
        if (Boolean.FALSE.equals(e.getActive())) throw new ResourceNotFoundException("Çalışan", id);
        return e;
    }

    private HairService findActiveHairService(Long id) {
        HairService s = hairServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hizmet", id));
        if (Boolean.FALSE.equals(s.getActive())) throw new ResourceNotFoundException("Hizmet", id);
        return s;
    }

    private void validateSameSalon(Customer customer, Employee employee, HairService hairService) {
        Long salonId = employee.getSalon().getId();
        if (!salonId.equals(hairService.getSalon().getId())) {
            throw new BadRequestException("Çalışan ve hizmet aynı salona ait olmalıdır.");
        }
        if (customer.getSalon() == null || !salonId.equals(customer.getSalon().getId())) {
            throw new BadRequestException("Müşteri ve randevu aynı salona ait olmalıdır.");
        }
    }

    private void requireAppointmentAccess(Appointment appointment) {
        salonAccessService.requireSalonAccess(appointment.getEmployee().getSalon().getId());
        if (salonAccessService.currentRole() == Role.EMPLOYEE
                && !appointment.getEmployee().getId().equals(salonAccessService.currentEmployeeId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Çalışan yalnızca kendi randevuları üzerinde işlem yapabilir.");
        }
    }

    private void requireEmployeeTargetOwnership(Employee employee) {
        if (salonAccessService.currentRole() == Role.EMPLOYEE
                && !employee.getId().equals(salonAccessService.currentEmployeeId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Çalışan başka bir çalışan adına randevu oluşturamaz veya güncelleyemez.");
        }
    }

    private int requirePositiveDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BadRequestException("Hizmet süresi pozitif bir dakika değeri olmalıdır.");
        }
        return durationMinutes;
    }

    private int effectiveDuration(Appointment appointment) {
        return requirePositiveDuration(appointment.getDurationMinutesSnapshot() != null
                ? appointment.getDurationMinutesSnapshot()
                : appointment.getHairService().getDurationMinutes());
    }

    private LocalDateTime requireStart(LocalDateTime start) {
        if (start == null) {
            throw new BadRequestException("appointmentDateTime alanı zorunludur.");
        }
        return start;
    }

    private boolean validateSchedule(
            Long salonId,
            LocalDateTime start,
            LocalDateTime end,
            AppointmentRequestDto request) {
        try {
            salonScheduleService.validateAppointment(salonId, start, end);
            return false;
        } catch (OutsideWorkingHoursException ex) {
            if (!Boolean.TRUE.equals(request.getOverrideOutsideWorkingHours())) {
                throw ex;
            }
            if (request.getOverrideReason() == null || request.getOverrideReason().isBlank()) {
                throw new BadRequestException("Çalışma saatleri dışı onay için overrideReason zorunludur.");
            }
            return true;
        }
    }

    private void applyScheduleOverride(Appointment appointment, boolean overridden, String reason) {
        appointment.setScheduleOverridden(overridden);
        appointment.setScheduleOverrideReason(overridden ? reason.trim() : null);
        appointment.setScheduleOverrideBy(overridden ? salonAccessService.currentUser() : null);
        appointment.setScheduleOverrideAt(overridden ? LocalDateTime.now() : null);
    }

    private void requireVersion(Appointment appointment, Long requestedVersion) {
        if (requestedVersion != null && !Objects.equals(requestedVersion, appointment.getVersion())) {
            throw new AppointmentConflictException(
                    "Randevu başka bir kullanıcı tarafından güncellendi; güncel veriyi yeniden yükleyin.");
        }
    }
}
