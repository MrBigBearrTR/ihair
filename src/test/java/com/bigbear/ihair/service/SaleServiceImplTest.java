package com.bigbear.ihair.service;

import com.bigbear.ihair.dto.request.CompleteSaleRequestDto;
import com.bigbear.ihair.dto.request.SaleItemRequestDto;
import com.bigbear.ihair.dto.request.SalePaymentRequestDto;
import com.bigbear.ihair.dto.request.SaleRequestDto;
import com.bigbear.ihair.entity.*;
import com.bigbear.ihair.entity.enums.PaymentMethod;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.entity.enums.SaleStatus;
import com.bigbear.ihair.repository.*;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.impl.SaleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    @Mock SaleRepository saleRepository;
    @Mock SalonRepository salonRepository;
    @Mock CustomerRepository customerRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock HairServiceRepository hairServiceRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock SalonAccessService salonAccessService;
    @InjectMocks SaleServiceImpl service;

    @Test
    void createCalculatesPricesAndDefaultsEmployeeForEmployeeUser() {
        Salon salon = salon();
        Customer customer = customer(salon);
        Employee employee = employee(salon);
        HairService hairService = hairService(salon, "15.25");
        User user = user(employee, Role.EMPLOYEE);
        SaleRequestDto request = new SaleRequestDto();
        request.setSalonId(1L);
        request.setCustomerId(2L);
        SaleItemRequestDto item = new SaleItemRequestDto();
        item.setServiceId(3L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        when(salonAccessService.resolveSalonId(1L)).thenReturn(1L);
        when(salonAccessService.currentUser()).thenReturn(user);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(hairServiceRepository.findById(3L)).thenReturn(Optional.of(hairService));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request);

        assertEquals(new BigDecimal("30.50"), response.getTotalAmount());
        assertEquals(new BigDecimal("15.25"), response.getItems().getFirst().getUnitPrice());
        assertEquals(4L, response.getItems().getFirst().getEmployeeId());
        assertEquals(SaleStatus.OPEN, response.getStatus());
        assertEquals(0, response.getPayments().size());
    }

    @Test
    void appointmentImportUsesFinalPriceSnapshot() {
        Salon salon = salon();
        Customer customer = customer(salon);
        Employee employee = employee(salon);
        HairService hairService = hairService(salon, "15.25");
        Appointment appointment = new Appointment();
        appointment.setId(9L);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCustomer(customer);
        appointment.setEmployee(employee);
        appointment.setHairService(hairService);
        appointment.setFinalPrice(new BigDecimal("8.50"));
        SaleRequestDto request = new SaleRequestDto();
        request.setSalonId(1L);
        request.setCustomerId(2L);
        request.setSourceAppointmentId(9L);

        when(salonAccessService.resolveSalonId(1L)).thenReturn(1L);
        when(salonAccessService.currentUser()).thenReturn(user(employee, Role.SALON_OWNER));
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(9L)).thenReturn(Optional.of(appointment));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request);

        assertEquals(new BigDecimal("8.50"), response.getTotalAmount());
        assertEquals(new BigDecimal("8.50"), response.getItems().getFirst().getUnitPrice());
        assertEquals(new BigDecimal("15.25"), response.getItems().getFirst().getListPrice());
    }

    @Test
    void createReusesOpenSaleAlreadyLinkedToAppointment() {
        Sale sale = openSale();
        Appointment appointment = new Appointment();
        appointment.setId(9L);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setCustomer(sale.getCustomer());
        appointment.setEmployee(sale.getItems().getFirst().getEmployee());
        appointment.setHairService(sale.getItems().getFirst().getService());
        appointment.setFinalPrice(new BigDecimal("10.00"));
        sale.setSourceAppointment(appointment);
        SaleRequestDto request = new SaleRequestDto();
        request.setSalonId(1L);
        request.setCustomerId(2L);
        request.setSourceAppointmentId(9L);

        when(salonAccessService.resolveSalonId(1L)).thenReturn(1L);
        when(salonAccessService.currentUser()).thenReturn(sale.getCreatedBy());
        when(salonRepository.findById(1L)).thenReturn(Optional.of(sale.getSalon()));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(sale.getCustomer()));
        when(saleRepository.findBySourceAppointmentId(9L)).thenReturn(Optional.of(sale));
        when(saleRepository.saveAndFlush(sale)).thenReturn(sale);

        var response = service.create(request);

        assertEquals(10L, response.getId());
        assertEquals(new BigDecimal("10.00"), response.getTotalAmount());
        verify(saleRepository, times(2)).saveAndFlush(sale);
    }

    @Test
    void completeWritesBackendTotalAndIsIdempotentAfterCompletion() {
        Sale sale = openSale();
        CompleteSaleRequestDto request = new CompleteSaleRequestDto();
        SalePaymentRequestDto payment = new SalePaymentRequestDto();
        payment.setMethod(PaymentMethod.CARD);
        request.setPayments(List.of(payment));
        when(saleRepository.findWithLockById(10L)).thenReturn(Optional.of(sale));
        when(saleRepository.saveAndFlush(sale)).thenReturn(sale);

        var first = service.complete(10L, request);

        assertEquals(SaleStatus.COMPLETED, first.getStatus());
        assertEquals(new BigDecimal("20.00"), first.getPayments().getFirst().getAmount());

        var second = service.complete(10L, request);

        assertSame(first.getPayments().getFirst().getMethod(), second.getPayments().getFirst().getMethod());
        verify(saleRepository, times(1)).saveAndFlush(sale);
        verify(saleRepository, times(2)).findWithLockById(10L);
    }

    @Test
    void completeMarksSourceAppointmentAsCompleted() {
        Sale sale = openSale();
        Appointment appointment = new Appointment();
        appointment.setId(9L);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        sale.setSourceAppointment(appointment);
        CompleteSaleRequestDto request = new CompleteSaleRequestDto();
        SalePaymentRequestDto payment = new SalePaymentRequestDto();
        payment.setMethod(PaymentMethod.CASH);
        request.setPayments(List.of(payment));
        when(saleRepository.findWithLockById(10L)).thenReturn(Optional.of(sale));
        when(saleRepository.saveAndFlush(sale)).thenReturn(sale);

        service.complete(10L, request);

        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void updateFlushesRemovedItemsBeforeReusingPositions() {
        Sale sale = openSale();
        SaleRequestDto request = new SaleRequestDto();
        request.setSalonId(1L);
        request.setCustomerId(2L);
        SaleItemRequestDto item = new SaleItemRequestDto();
        item.setServiceId(3L);
        item.setEmployeeId(4L);
        item.setQuantity(1);
        item.setPosition(0);
        request.setItems(List.of(item));

        when(saleRepository.findWithLockById(10L)).thenReturn(Optional.of(sale));
        when(salonAccessService.resolveSalonId(1L)).thenReturn(1L);
        when(salonAccessService.currentUser()).thenReturn(sale.getCreatedBy());
        when(customerRepository.findById(2L)).thenReturn(Optional.of(sale.getCustomer()));
        when(hairServiceRepository.findById(3L)).thenReturn(
                Optional.of(sale.getItems().getFirst().getService()));
        when(employeeRepository.findById(4L)).thenReturn(
                Optional.of(sale.getItems().getFirst().getEmployee()));
        when(saleRepository.saveAndFlush(sale)).thenReturn(sale);

        var response = service.update(10L, request);

        assertEquals(new BigDecimal("10.00"), response.getTotalAmount());
        verify(saleRepository, times(2)).saveAndFlush(sale);
    }

    @Test
    void availableAppointmentsIncludeConfirmedAndCompletedStatuses() {
        Salon salon = salon();
        when(salonAccessService.resolveSalonId(1L)).thenReturn(1L);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(appointmentRepository.findAvailableForSale(
                1L, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED)))
                .thenReturn(List.of());

        service.getAvailableAppointments(1L);

        verify(appointmentRepository).findAvailableForSale(
                1L, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED));
    }

    @Test
    void listFiltersByCompletedDateAndEmployee() {
        Sale included = openSale();
        included.setStatus(SaleStatus.COMPLETED);
        included.setCompletedAt(LocalDateTime.of(2026, 7, 20, 15, 0));
        included.setSubtotal(new BigDecimal("20.00"));
        included.setTotalAmount(new BigDecimal("20.00"));
        Sale excluded = openSale();
        excluded.setId(20L);
        excluded.setStatus(SaleStatus.COMPLETED);
        excluded.setCompletedAt(LocalDateTime.of(2026, 7, 19, 15, 0));
        excluded.setSubtotal(new BigDecimal("20.00"));
        excluded.setTotalAmount(new BigDecimal("20.00"));
        Set<Long> salonIds = Set.of(1L);
        when(salonAccessService.resolveSalonIdsForList(1L)).thenReturn(salonIds);
        when(saleRepository.findAllBySalonIdInAndStatusOrderByCreatedAtDesc(
                salonIds, SaleStatus.COMPLETED)).thenReturn(List.of(included, excluded));
        when(employeeRepository.findByIdAndSalonIdInAndActiveTrue(4L, salonIds))
                .thenReturn(Optional.of(included.getItems().getFirst().getEmployee()));

        var response = service.getAll(
                1L, SaleStatus.COMPLETED,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20), 4L);

        assertEquals(1, response.size());
        assertEquals(10L, response.getFirst().getId());
    }

    private Sale openSale() {
        Salon salon = salon();
        Sale sale = new Sale();
        sale.setId(10L);
        sale.setSalon(salon);
        sale.setCustomer(customer(salon));
        sale.setCreatedBy(user(employee(salon), Role.SALON_OWNER));
        SaleItem item = new SaleItem();
        item.setId(11L);
        item.setService(hairService(salon, "10.00"));
        item.setEmployee(employee(salon));
        item.setQuantity(2);
        item.setPosition(0);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setListPrice(new BigDecimal("10.00"));
        item.setLineTotal(new BigDecimal("20.00"));
        item.setServiceNameSnapshot("Kesim");
        item.setEmployeeNameSnapshot("Ada Usta");
        sale.addItem(item);
        return sale;
    }

    private Salon salon() {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setName("Merkez");
        return salon;
    }

    private Customer customer(Salon salon) {
        Customer customer = new Customer();
        customer.setId(2L);
        customer.setFirstName("Müşteri");
        customer.setLastName("Bir");
        customer.setSalon(salon);
        return customer;
    }

    private Employee employee(Salon salon) {
        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFirstName("Ada");
        employee.setLastName("Usta");
        employee.setSalon(salon);
        return employee;
    }

    private HairService hairService(Salon salon, String price) {
        HairService serviceEntity = new HairService();
        serviceEntity.setId(3L);
        serviceEntity.setName("Kesim");
        serviceEntity.setPrice(new BigDecimal(price));
        serviceEntity.setSalon(salon);
        return serviceEntity;
    }

    private User user(Employee employee, Role role) {
        User user = new User();
        user.setId(5L);
        user.setUsername("kullanici");
        user.setRole(role);
        user.setEmployee(employee);
        return user;
    }
}
