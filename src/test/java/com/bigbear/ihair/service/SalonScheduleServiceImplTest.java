package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.schedule.ScheduleDayDto;
import com.bigbear.ihair.dto.schedule.SalonScheduleRequestDto;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.SalonSchedule;
import com.bigbear.ihair.entity.SalonScheduleDay;
import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.OutsideWorkingHoursException;
import com.bigbear.ihair.repository.SalonHolidayRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.repository.SalonScheduleRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.impl.SalonScheduleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonScheduleServiceImplTest {

    @Mock SalonScheduleRepository scheduleRepository;
    @Mock SalonHolidayRepository holidayRepository;
    @Mock SalonRepository salonRepository;
    @Mock SalonAccessService salonAccessService;
    @InjectMocks SalonScheduleServiceImpl service;

    @Test
    void getScheduleReturnsPermissiveLegacyDefaults() {
        Salon salon = salon();
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(scheduleRepository.findBySalonId(1L)).thenReturn(Optional.empty());
        when(holidayRepository.findAllBySalonIdOrderByStartDateAsc(1L)).thenReturn(List.of());

        var response = service.getSchedule(1L);

        assertFalse(response.isConfigured());
        assertEquals("Europe/Istanbul", response.getTimeZone());
        assertEquals(7, response.getDays().size());
        assertTrue(response.getDays().stream()
                .filter(day -> day.getDayOfWeek() == DayOfWeek.SUNDAY)
                .findFirst().orElseThrow().getClosed());
    }

    @Test
    void updateScheduleRequiresEveryDayExactlyOnce() {
        SalonScheduleRequestDto request = mock(SalonScheduleRequestDto.class);
        when(request.getDays()).thenReturn(List.of(
                new ScheduleDayDto(DayOfWeek.MONDAY, false, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        Salon salon = salon();
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        User owner = new User();
        owner.setRole(Role.SALON_OWNER);
        when(salonAccessService.currentUser()).thenReturn(owner);

        assertThrows(BadRequestException.class, () -> service.updateSchedule(1L, request));
        verify(scheduleRepository, never()).saveAndFlush(any());
    }

    @Test
    void configuredScheduleRejectsOutsideOpenInterval() {
        when(scheduleRepository.findBySalonId(1L)).thenReturn(Optional.of(mondaySchedule()));

        OutsideWorkingHoursException exception = assertThrows(
                OutsideWorkingHoursException.class,
                () -> service.validateAppointment(
                        1L,
                        LocalDateTime.of(2026, 7, 20, 8, 30),
                        LocalDateTime.of(2026, 7, 20, 9, 30)));

        assertEquals("OUTSIDE_OPEN_INTERVAL", exception.getReason());
    }

    @Test
    void defaultScheduleAlsoRejectsOutsideOpenInterval() {
        when(scheduleRepository.findBySalonId(1L)).thenReturn(Optional.empty());

        OutsideWorkingHoursException exception = assertThrows(
                OutsideWorkingHoursException.class,
                () -> service.validateAppointment(
                        1L,
                        LocalDateTime.of(2026, 7, 20, 8, 30),
                        LocalDateTime.of(2026, 7, 20, 9, 30)));

        assertEquals("OUTSIDE_OPEN_INTERVAL", exception.getReason());
    }

    @Test
    void configuredScheduleRejectsOverlappingHolidayRange() {
        when(scheduleRepository.findBySalonId(1L)).thenReturn(Optional.of(mondaySchedule()));
        when(holidayRepository.existsBySalonIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                anyLong(), any(), any())).thenReturn(true);

        OutsideWorkingHoursException exception = assertThrows(
                OutsideWorkingHoursException.class,
                () -> service.validateAppointment(
                        1L,
                        LocalDateTime.of(2026, 7, 20, 10, 0),
                        LocalDateTime.of(2026, 7, 20, 11, 0)));

        assertEquals("HOLIDAY", exception.getReason());
    }

    private SalonSchedule mondaySchedule() {
        SalonSchedule schedule = new SalonSchedule();
        schedule.setConfigured(true);
        SalonScheduleDay monday = new SalonScheduleDay();
        monday.setSchedule(schedule);
        monday.setDayOfWeek(DayOfWeek.MONDAY);
        monday.setClosed(false);
        monday.setOpensAt(LocalTime.of(9, 0));
        monday.setClosesAt(LocalTime.of(19, 0));
        schedule.getDays().add(monday);
        return schedule;
    }

    private Salon salon() {
        Salon salon = new Salon();
        salon.setId(1L);
        return salon;
    }
}
