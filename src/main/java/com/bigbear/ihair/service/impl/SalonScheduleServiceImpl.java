package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.schedule.*;
import com.bigbear.ihair.entity.*;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.OutsideWorkingHoursException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.SalonHolidayRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.repository.SalonScheduleRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.SalonScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SalonScheduleServiceImpl implements SalonScheduleService {

    public static final String DEFAULT_TIME_ZONE = "Europe/Istanbul";
    private static final LocalTime DEFAULT_OPEN = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(19, 0);

    private final SalonScheduleRepository scheduleRepository;
    private final SalonHolidayRepository holidayRepository;
    private final SalonRepository salonRepository;
    private final SalonAccessService salonAccessService;

    @Override
    @Transactional(readOnly = true)
    public SalonScheduleResponseDto getSchedule(Long salonId) {
        requireReadAccess(salonId);
        return response(salonId, scheduleRepository.findBySalonId(salonId).orElse(null));
    }

    @Override
    @Transactional
    public SalonScheduleResponseDto updateSchedule(Long salonId, SalonScheduleRequestDto request) {
        requireWriteAccess(salonId);
        Salon salon = findSalon(salonId);
        validateRequest(request);
        SalonSchedule schedule = scheduleRepository.findBySalonId(salonId).orElseGet(SalonSchedule::new);
        schedule.setSalon(salon);
        schedule.setTimeZone(normalizeTimeZone(request.getTimeZone()));
        schedule.setConfigured(true);
        Map<DayOfWeek, SalonScheduleDay> existingDays = new EnumMap<>(DayOfWeek.class);
        schedule.getDays().forEach(day -> existingDays.put(day.getDayOfWeek(), day));
        for (ScheduleDayDto value : request.getDays()) {
            SalonScheduleDay day = existingDays.getOrDefault(
                    value.getDayOfWeek(), new SalonScheduleDay());
            day.setSchedule(schedule);
            day.setDayOfWeek(value.getDayOfWeek());
            day.setClosed(Boolean.TRUE.equals(value.getClosed()));
            day.setOpensAt(day.getClosed() ? null : value.getOpensAt());
            day.setClosesAt(day.getClosed() ? null : value.getClosesAt());
            if (day.getId() == null && !schedule.getDays().contains(day)) {
                schedule.getDays().add(day);
            }
        }
        return response(salonId, scheduleRepository.saveAndFlush(schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonHolidayDto> getHolidays(Long salonId) {
        requireReadAccess(salonId);
        return holidayRepository.findAllBySalonIdOrderByStartDateAsc(salonId)
                .stream().map(SalonHolidayDto::new).toList();
    }

    @Override
    @Transactional
    public SalonHolidayDto createHoliday(Long salonId, SalonHolidayRequestDto request) {
        requireWriteAccess(salonId);
        SalonHoliday holiday = new SalonHoliday();
        holiday.setSalon(findSalon(salonId));
        applyHoliday(holiday, request);
        return new SalonHolidayDto(holidayRepository.save(holiday));
    }

    @Override
    @Transactional
    public SalonHolidayDto updateHoliday(Long salonId, Long holidayId, SalonHolidayRequestDto request) {
        requireWriteAccess(salonId);
        SalonHoliday holiday = findHoliday(salonId, holidayId);
        applyHoliday(holiday, request);
        return new SalonHolidayDto(holidayRepository.save(holiday));
    }

    @Override
    @Transactional
    public void deleteHoliday(Long salonId, Long holidayId) {
        requireWriteAccess(salonId);
        holidayRepository.delete(findHoliday(salonId, holidayId));
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = OutsideWorkingHoursException.class)
    public void validateAppointment(Long salonId, LocalDateTime start, LocalDateTime end) {
        SalonSchedule schedule = scheduleRepository.findBySalonId(salonId).orElse(null);
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BadRequestException("Randevu başlangıç ve bitiş zamanı geçersiz.");
        }
        SalonScheduleDay day = resolveDay(schedule, start.getDayOfWeek());
        String reason = null;
        if (!start.toLocalDate().equals(end.minusNanos(1).toLocalDate())) {
            reason = "APPOINTMENT_SPANS_MULTIPLE_DAYS";
        } else if (day == null || Boolean.TRUE.equals(day.getClosed())) {
            reason = "SALON_CLOSED";
        } else if (start.toLocalTime().isBefore(day.getOpensAt())
                || end.toLocalTime().isAfter(day.getClosesAt())) {
            reason = "OUTSIDE_OPEN_INTERVAL";
        } else if (holidayRepository.existsBySalonIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                salonId, end.minusNanos(1).toLocalDate(), start.toLocalDate())) {
            reason = "HOLIDAY";
        }
        if (reason != null) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("salonId", salonId);
            details.put("start", start);
            details.put("end", end);
            details.put("dayOfWeek", start.getDayOfWeek());
            if (day != null) {
                details.put("opensAt", day.getOpensAt());
                details.put("closesAt", day.getClosesAt());
            }
            throw new OutsideWorkingHoursException(
                    "Randevu salon çalışma saatleri dışındadır; devam etmek için onay gereklidir.",
                    reason,
                    details);
        }
    }

    private SalonScheduleDay resolveDay(SalonSchedule schedule, DayOfWeek dayOfWeek) {
        if (schedule != null) {
            Optional<SalonScheduleDay> configuredDay = schedule.getDays().stream()
                    .filter(value -> value.getDayOfWeek() == dayOfWeek)
                    .findFirst();
            if (configuredDay.isPresent()) {
                return configuredDay.get();
            }
        }
        SalonScheduleDay fallback = new SalonScheduleDay();
        fallback.setDayOfWeek(dayOfWeek);
        fallback.setClosed(dayOfWeek == DayOfWeek.SUNDAY);
        fallback.setOpensAt(fallback.getClosed() ? null : DEFAULT_OPEN);
        fallback.setClosesAt(fallback.getClosed() ? null : DEFAULT_CLOSE);
        return fallback;
    }

    @Override
    @Transactional(readOnly = true)
    public String getTimeZone(Long salonId) {
        return scheduleRepository.findBySalonId(salonId)
                .map(SalonSchedule::getTimeZone).orElse(DEFAULT_TIME_ZONE);
    }

    private SalonScheduleResponseDto response(Long salonId, SalonSchedule schedule) {
        List<ScheduleDayDto> days = schedule == null || schedule.getDays().isEmpty()
                ? defaultDays()
                : schedule.getDays().stream()
                        .sorted(Comparator.comparingInt(day -> day.getDayOfWeek().getValue()))
                        .map(ScheduleDayDto::new).toList();
        return new SalonScheduleResponseDto(
                salonId,
                schedule == null ? DEFAULT_TIME_ZONE : schedule.getTimeZone(),
                schedule != null && Boolean.TRUE.equals(schedule.getConfigured()),
                days,
                holidayRepository.findAllBySalonIdOrderByStartDateAsc(salonId)
                        .stream().map(SalonHolidayDto::new).toList());
    }

    private List<ScheduleDayDto> defaultDays() {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> day == DayOfWeek.SUNDAY
                        ? new ScheduleDayDto(day, true, null, null)
                        : new ScheduleDayDto(day, false, DEFAULT_OPEN, DEFAULT_CLOSE))
                .toList();
    }

    private void validateRequest(SalonScheduleRequestDto request) {
        if (request == null || request.getDays() == null || request.getDays().size() != 7) {
            throw new BadRequestException("Program MONDAY-SUNDAY tüm yedi günü içermelidir.");
        }
        Set<DayOfWeek> unique = EnumSet.noneOf(DayOfWeek.class);
        for (ScheduleDayDto day : request.getDays()) {
            if (day == null || day.getDayOfWeek() == null || !unique.add(day.getDayOfWeek())) {
                throw new BadRequestException("Her gün programda tam olarak bir kez bulunmalıdır.");
            }
            boolean closed = Boolean.TRUE.equals(day.getClosed());
            if (!closed && (day.getOpensAt() == null || day.getClosesAt() == null
                    || !day.getClosesAt().isAfter(day.getOpensAt()))) {
                throw new BadRequestException("Açık günlerde opensAt, closesAt değerleri ve geçerli aralık zorunludur.");
            }
        }
        normalizeTimeZone(request.getTimeZone());
    }

    private String normalizeTimeZone(String value) {
        String zone = value == null || value.isBlank() ? DEFAULT_TIME_ZONE : value.trim();
        try {
            return ZoneId.of(zone).getId();
        } catch (DateTimeException ex) {
            throw new BadRequestException("Geçersiz timeZone: " + zone);
        }
    }

    private void applyHoliday(SalonHoliday holiday, SalonHolidayRequestDto request) {
        if (request == null || request.getStartDate() == null || request.getEndDate() == null
                || request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Tatil startDate/endDate aralığı geçersiz.");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("Tatil reason alanı zorunludur.");
        }
        holiday.setStartDate(request.getStartDate());
        holiday.setEndDate(request.getEndDate());
        holiday.setReason(request.getReason().trim());
    }

    private Salon findSalon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", salonId));
    }

    private SalonHoliday findHoliday(Long salonId, Long holidayId) {
        SalonHoliday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Tatil", holidayId));
        if (!salonId.equals(holiday.getSalon().getId())) {
            throw new ResourceNotFoundException("Tatil", holidayId);
        }
        return holiday;
    }

    private void requireReadAccess(Long salonId) {
        findSalon(salonId);
        salonAccessService.requireSalonAccess(salonId);
    }

    private void requireWriteAccess(Long salonId) {
        requireReadAccess(salonId);
        if (salonAccessService.currentUser().getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Çalışanlar salon programını değiştiremez.");
        }
    }
}
