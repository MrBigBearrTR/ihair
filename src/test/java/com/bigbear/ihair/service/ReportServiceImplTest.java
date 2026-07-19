package com.bigbear.ihair.service;

import com.bigbear.ihair.entity.*;
import com.bigbear.ihair.entity.enums.PaymentMethod;
import com.bigbear.ihair.entity.enums.RevenueGroupBy;
import com.bigbear.ihair.entity.enums.SaleStatus;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.EmployeeRepository;
import com.bigbear.ihair.repository.SaleRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock SaleRepository saleRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock SalonAccessService salonAccessService;
    @InjectMocks ReportServiceImpl service;

    @Test
    void employeeGroupingUsesItemRevenueAndSummaryUsesCompletedSaleTotals() {
        Sale sale = completedSale();
        when(salonAccessService.resolveSalonIdsForList(1L)).thenReturn(Set.of(1L));
        when(saleRepository.findForRevenueBySalonIds(
                any(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(sale));

        var report = service.getRevenue(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                RevenueGroupBy.EMPLOYEE,
                null);

        assertEquals(new BigDecimal("60.00"), report.getTotalRevenue());
        assertEquals(1, report.getSaleCount());
        assertEquals(3, report.getItemCount());
        assertEquals(new BigDecimal("60.00"), report.getAverageSale());
        assertEquals(new BigDecimal("60.00"), report.getPaymentBreakdown().get(PaymentMethod.CASH));
        assertEquals(new BigDecimal("60.00"), report.getGroups().getFirst().getRevenue());
        assertEquals(3, report.getGroups().getFirst().getItemCount());
        verify(saleRepository).findForRevenueBySalonIds(
                org.mockito.ArgumentMatchers.eq(SaleStatus.COMPLETED),
                org.mockito.ArgumentMatchers.eq(Set.of(1L)),
                any(LocalDateTime.class),
                any(LocalDateTime.class));
    }

    @Test
    void employeeFilterUsesOnlyMatchingItemsForSummaryGroupsAndPayments() {
        Sale sale = completedSale();
        Employee selectedEmployee = sale.getItems().getFirst().getEmployee();
        addOtherEmployeeItem(sale);
        sale.setSubtotal(new BigDecimal("100.00"));
        sale.setTotalAmount(new BigDecimal("100.00"));
        sale.getPayments().getFirst().setAmount(new BigDecimal("100.00"));
        when(salonAccessService.resolveSalonIdsForList(1L)).thenReturn(Set.of(1L));
        when(employeeRepository.findByIdAndSalonIdInAndActiveTrue(2L, Set.of(1L)))
                .thenReturn(java.util.Optional.of(selectedEmployee));
        when(saleRepository.findForRevenueBySalonIds(
                any(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(sale));

        var dayReport = service.getRevenue(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                RevenueGroupBy.DAY,
                2L);

        assertEquals(new BigDecimal("60.00"), dayReport.getTotalRevenue());
        assertEquals(1, dayReport.getSaleCount());
        assertEquals(3, dayReport.getItemCount());
        assertEquals(new BigDecimal("60.00"), dayReport.getAverageSale());
        assertEquals(new BigDecimal("60.00"), dayReport.getPaymentBreakdown().get(PaymentMethod.CASH));
        assertEquals(new BigDecimal("60.00"), dayReport.getGroups().getFirst().getRevenue());
        assertEquals(3, dayReport.getGroups().getFirst().getItemCount());

        var monthReport = service.getRevenue(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                RevenueGroupBy.MONTH,
                2L);

        assertEquals("2026-07", monthReport.getGroups().getFirst().getKey());
        assertEquals(new BigDecimal("60.00"), monthReport.getGroups().getFirst().getRevenue());

        var employeeReport = service.getRevenue(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                RevenueGroupBy.EMPLOYEE,
                2L);

        assertEquals(1, employeeReport.getGroups().size());
        assertEquals(2L, employeeReport.getGroups().getFirst().getEmployeeId());
        assertEquals(new BigDecimal("60.00"), employeeReport.getGroups().getFirst().getRevenue());
    }

    @Test
    void employeeFilterRejectsEmployeeOutsideAuthorizedSalonScope() {
        when(salonAccessService.resolveSalonIdsForList(1L)).thenReturn(Set.of(1L));
        when(employeeRepository.findByIdAndSalonIdInAndActiveTrue(99L, Set.of(1L)))
                .thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getRevenue(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                RevenueGroupBy.DAY,
                99L));
        verifyNoInteractions(saleRepository);
    }

    private Sale completedSale() {
        Salon salon = new Salon();
        salon.setId(1L);
        Employee employee = new Employee();
        employee.setId(2L);
        employee.setFirstName("Ece");
        employee.setLastName("Usta");
        employee.setSalon(salon);
        HairService hairService = new HairService();
        hairService.setId(3L);
        hairService.setName("Boya");
        hairService.setSalon(salon);

        Sale sale = new Sale();
        sale.setId(4L);
        sale.setSalon(salon);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setCompletedAt(LocalDateTime.of(2026, 7, 10, 12, 0));
        sale.setSubtotal(new BigDecimal("60.00"));
        sale.setTotalAmount(new BigDecimal("60.00"));

        SaleItem item = new SaleItem();
        item.setService(hairService);
        item.setEmployee(employee);
        item.setQuantity(3);
        item.setPosition(0);
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setListPrice(new BigDecimal("20.00"));
        item.setLineTotal(new BigDecimal("60.00"));
        item.setDiscountShare(BigDecimal.ZERO);
        item.setNetLineTotal(new BigDecimal("60.00"));
        item.setServiceNameSnapshot("Boya");
        item.setEmployeeNameSnapshot("Ece Usta");
        sale.addItem(item);

        SalePayment payment = new SalePayment();
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(new BigDecimal("60.00"));
        sale.addPayment(payment);
        return sale;
    }

    private void addOtherEmployeeItem(Sale sale) {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setFirstName("Başka");
        employee.setLastName("Çalışan");
        employee.setSalon(sale.getSalon());
        HairService service = new HairService();
        service.setId(6L);
        service.setName("Bakım");
        service.setSalon(sale.getSalon());
        SaleItem item = new SaleItem();
        item.setService(service);
        item.setEmployee(employee);
        item.setQuantity(1);
        item.setPosition(1);
        item.setUnitPrice(new BigDecimal("40.00"));
        item.setListPrice(new BigDecimal("40.00"));
        item.setLineTotal(new BigDecimal("40.00"));
        item.setDiscountShare(BigDecimal.ZERO);
        item.setNetLineTotal(new BigDecimal("40.00"));
        item.setServiceNameSnapshot("Bakım");
        item.setEmployeeNameSnapshot("Başka Çalışan");
        sale.addItem(item);
    }
}
