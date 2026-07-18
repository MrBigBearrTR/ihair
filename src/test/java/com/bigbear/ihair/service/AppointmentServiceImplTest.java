package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.AppointmentRequestDto;
import com.bigbear.ihair.dto.request.AppointmentStatusRequestDto;
import com.bigbear.ihair.entity.Appointment;
import com.bigbear.ihair.entity.Customer;
import com.bigbear.ihair.entity.Employee;
import com.bigbear.ihair.entity.HairService;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.AppointmentConflictException;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.OutsideWorkingHoursException;
import com.bigbear.ihair.repository.AppointmentRepository;
import com.bigbear.ihair.repository.CampaignRepository;
import com.bigbear.ihair.repository.CustomerRepository;
import com.bigbear.ihair.repository.EmployeeRepository;
import com.bigbear.ihair.repository.HairServiceRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock CustomerRepository customerRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock HairServiceRepository hairServiceRepository;
    @Mock CampaignRepository campaignRepository;
    @Mock SalonAccessService salonAccessService;
    @Mock SalonScheduleService salonScheduleService;
    @InjectMocks AppointmentServiceImpl service;

    @Test
    void updateStatusOnlyChangesStatusAndDoesNotApplyCampaign() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 7, 20, 10, 0), 60);
        appointment.setId(10L);
        AppointmentStatusRequestDto request = mock(AppointmentStatusRequestDto.class);
        when(request.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(employeeRepository.findWithLockById(1L)).thenReturn(Optional.of(appointment.getEmployee()));
        when(appointmentRepository.findAllByEmployeeIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        service.updateStatus(10L, request);

        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        verifyNoInteractions(campaignRepository);
    }

    @Test
    void createRejectsOverlappingHalfOpenIntervals() {
        Salon salon = salon();
        Employee employee = employee(salon);
        HairService serviceEntity = hairService(salon, 60);
        Customer customer = customer(salon);
        Appointment existing = appointment(LocalDateTime.of(2026, 7, 20, 10, 0), 60);

        AppointmentRequestDto request = mock(AppointmentRequestDto.class);
        when(request.getCustomerId()).thenReturn(1L);
        when(request.getEmployeeId()).thenReturn(1L);
        when(request.getHairServiceId()).thenReturn(1L);
        when(request.getAppointmentDateTime()).thenReturn(LocalDateTime.of(2026, 7, 20, 10, 30));
        when(request.getStatus()).thenReturn(AppointmentStatus.PENDING);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(employeeRepository.findWithLockById(1L)).thenReturn(Optional.of(employee));
        when(hairServiceRepository.findById(1L)).thenReturn(Optional.of(serviceEntity));
        when(appointmentRepository.findAllByEmployeeIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of(existing));

        assertThrows(AppointmentConflictException.class, () -> service.create(request));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createRejectsNullServiceDuration() {
        assertInvalidDuration(null);
    }

    @Test
    void createRejectsZeroServiceDuration() {
        assertInvalidDuration(0);
    }

    @Test
    void createRequiresConfirmationOutsideWorkingHours() {
        Salon salon = salon();
        Employee employee = employee(salon);
        HairService hairService = hairService(salon, 60);
        Customer customer = customer(salon);
        AppointmentRequestDto request = appointmentRequest(LocalDateTime.of(2026, 7, 20, 8, 0));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(employeeRepository.findWithLockById(1L)).thenReturn(Optional.of(employee));
        when(hairServiceRepository.findById(1L)).thenReturn(Optional.of(hairService));
        when(appointmentRepository.findAllByEmployeeIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        doThrow(new OutsideWorkingHoursException("Onay gerekli", "OUTSIDE_OPEN_INTERVAL", Map.of()))
                .when(salonScheduleService).validateAppointment(
                        eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));

        assertThrows(OutsideWorkingHoursException.class, () -> service.create(request));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAuditsConfirmedOutsideWorkingHoursOverride() {
        Salon salon = salon();
        Employee employee = employee(salon);
        HairService hairService = hairService(salon, 60);
        Customer customer = customer(salon);
        AppointmentRequestDto request = appointmentRequest(LocalDateTime.of(2026, 7, 20, 8, 0));
        when(request.getOverrideOutsideWorkingHours()).thenReturn(true);
        when(request.getOverrideReason()).thenReturn("Müşteri talebi");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(employeeRepository.findWithLockById(1L)).thenReturn(Optional.of(employee));
        when(hairServiceRepository.findById(1L)).thenReturn(Optional.of(hairService));
        when(appointmentRepository.findAllByEmployeeIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        doThrow(new OutsideWorkingHoursException("Onay gerekli", "OUTSIDE_OPEN_INTERVAL", Map.of()))
                .when(salonScheduleService).validateAppointment(
                        eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
        User user = new User();
        user.setId(99L);
        when(salonAccessService.currentUser()).thenReturn(user);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        Appointment saved = verifyAndCaptureSavedAppointment();
        assertEquals(true, saved.getScheduleOverridden());
        assertEquals("Müşteri talebi", saved.getScheduleOverrideReason());
        assertEquals(99L, saved.getScheduleOverrideBy().getId());
    }

    @Test
    void weekRequiresSalonIdForOwner() {
        when(salonAccessService.currentRole()).thenReturn(Role.SALON_OWNER);

        assertThrows(BadRequestException.class,
                () -> service.getWeek(null, java.time.LocalDate.of(2026, 7, 20)));
    }

    @Test
    void employeeCannotReadAnotherEmployeesAppointment() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 7, 20, 10, 0), 60);
        appointment.setId(10L);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(salonAccessService.currentRole()).thenReturn(Role.EMPLOYEE);
        when(salonAccessService.currentEmployeeId()).thenReturn(2L);

        assertThrows(AccessDeniedException.class, () -> service.getById(10L));
    }

    private void assertInvalidDuration(Integer duration) {
        Salon salon = salon();
        Employee employee = employee(salon);
        HairService serviceEntity = hairService(salon, 30);
        serviceEntity.setDurationMinutes(duration);
        Customer customer = customer(salon);
        AppointmentRequestDto request = mock(AppointmentRequestDto.class);
        when(request.getCustomerId()).thenReturn(1L);
        when(request.getEmployeeId()).thenReturn(1L);
        when(request.getHairServiceId()).thenReturn(1L);
        when(request.getAppointmentDateTime()).thenReturn(LocalDateTime.of(2026, 7, 20, 10, 0));
        when(request.getStatus()).thenReturn(AppointmentStatus.PENDING);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(employeeRepository.findWithLockById(1L)).thenReturn(Optional.of(employee));
        when(hairServiceRepository.findById(1L)).thenReturn(Optional.of(serviceEntity));

        assertThrows(BadRequestException.class, () -> service.create(request));
        verify(appointmentRepository, never()).save(any());
    }

    private AppointmentRequestDto appointmentRequest(LocalDateTime start) {
        AppointmentRequestDto request = mock(AppointmentRequestDto.class);
        when(request.getCustomerId()).thenReturn(1L);
        when(request.getEmployeeId()).thenReturn(1L);
        when(request.getHairServiceId()).thenReturn(1L);
        when(request.getAppointmentDateTime()).thenReturn(start);
        when(request.getStatus()).thenReturn(AppointmentStatus.PENDING);
        return request;
    }

    private Appointment verifyAndCaptureSavedAppointment() {
        var captor = org.mockito.ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        return captor.getValue();
    }

    private Appointment appointment(LocalDateTime start, int duration) {
        Salon salon = salon();
        Appointment appointment = new Appointment();
        appointment.setEmployee(employee(salon));
        appointment.setHairService(hairService(salon, duration));
        appointment.setCustomer(customer(salon));
        appointment.setAppointmentDateTime(start);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setFinalPrice(BigDecimal.TEN);
        return appointment;
    }

    private Salon salon() {
        Salon salon = new Salon();
        salon.setId(1L);
        return salon;
    }

    private Employee employee(Salon salon) {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setSalon(salon);
        return employee;
    }

    private HairService hairService(Salon salon, int duration) {
        HairService hairService = new HairService();
        hairService.setId(1L);
        hairService.setSalon(salon);
        hairService.setDurationMinutes(duration);
        hairService.setPrice(BigDecimal.TEN);
        return hairService;
    }

    private Customer customer(Salon salon) {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setSalon(salon);
        return customer;
    }
}
