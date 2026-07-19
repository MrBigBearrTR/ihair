package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.response.RevenueGroupResponseDto;
import com.bigbear.ihair.dto.response.RevenueReportResponseDto;
import com.bigbear.ihair.entity.Employee;
import com.bigbear.ihair.entity.Sale;
import com.bigbear.ihair.entity.SaleItem;
import com.bigbear.ihair.entity.enums.PaymentMethod;
import com.bigbear.ihair.entity.enums.RevenueGroupBy;
import com.bigbear.ihair.entity.enums.SaleStatus;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.EmployeeRepository;
import com.bigbear.ihair.repository.SaleRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final SaleRepository saleRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonAccessService salonAccessService;

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponseDto getRevenue(
            Long salonId,
            LocalDate from,
            LocalDate to,
            RevenueGroupBy groupBy,
            Long employeeId) {
        validate(from, to, groupBy);
        Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
        validateEmployee(employeeId, salonIds);
        List<Sale> sales = salonIds == null
                ? saleRepository.findAllForRevenue(
                        SaleStatus.COMPLETED, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                : saleRepository.findForRevenueBySalonIds(
                        SaleStatus.COMPLETED, salonIds, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        List<Sale> filteredSales = employeeId == null
                ? sales
                : sales.stream().filter(sale -> !matchingItems(sale, employeeId).isEmpty()).toList();
        BigDecimal totalRevenue = filteredSales.stream()
                .map(sale -> employeeId == null ? sale.getTotalAmount() : itemRevenue(sale, employeeId))
                .reduce(ZERO, BigDecimal::add);
        long itemCount = filteredSales.stream()
                .flatMap(sale -> matchingItems(sale, employeeId).stream())
                .mapToLong(SaleItem::getQuantity)
                .sum();
        BigDecimal averageSale = filteredSales.isEmpty()
                ? ZERO
                : totalRevenue.divide(BigDecimal.valueOf(filteredSales.size()), 2, RoundingMode.HALF_UP);

        Map<PaymentMethod, BigDecimal> paymentBreakdown = new EnumMap<>(PaymentMethod.class);
        Arrays.stream(PaymentMethod.values()).forEach(method -> paymentBreakdown.put(method, ZERO));
        if (employeeId == null) {
            filteredSales.stream().flatMap(sale -> sale.getPayments().stream()).forEach(payment ->
                    paymentBreakdown.merge(payment.getMethod(), payment.getAmount(), BigDecimal::add));
        } else {
            filteredSales.forEach(sale -> {
                PaymentMethod method = sale.getPayments().stream().findFirst()
                        .orElseThrow(() -> new BadRequestException(
                                "Tamamlanmış satışın ödeme kaydı bulunmuyor."))
                        .getMethod();
                paymentBreakdown.merge(method, itemRevenue(sale, employeeId), BigDecimal::add);
            });
        }
        paymentBreakdown.replaceAll((method, amount) -> money(amount));

        List<RevenueGroupResponseDto> groups = groupBy == RevenueGroupBy.EMPLOYEE
                ? groupByEmployee(filteredSales, employeeId)
                : groupByPeriod(filteredSales, groupBy, employeeId);

        return new RevenueReportResponseDto(
                salonId,
                from,
                to,
                groupBy,
                money(totalRevenue),
                filteredSales.size(),
                itemCount,
                averageSale,
                paymentBreakdown,
                groups);
    }

    private List<RevenueGroupResponseDto> groupByPeriod(
            List<Sale> sales,
            RevenueGroupBy groupBy,
            Long employeeId) {
        DateTimeFormatter formatter = groupBy == RevenueGroupBy.DAY
                ? DateTimeFormatter.ISO_LOCAL_DATE
                : DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, MutableGroup> grouped = new TreeMap<>();
        for (Sale sale : sales) {
            String key = sale.getCompletedAt().format(formatter);
            MutableGroup group = grouped.computeIfAbsent(key, value -> new MutableGroup(value, value, null));
            group.revenue = group.revenue.add(
                    employeeId == null ? sale.getTotalAmount() : itemRevenue(sale, employeeId));
            group.saleIds.add(sale.getId());
            group.itemCount += matchingItems(sale, employeeId).stream()
                    .mapToLong(SaleItem::getQuantity).sum();
        }
        return grouped.values().stream().map(MutableGroup::toResponse).toList();
    }

    private List<RevenueGroupResponseDto> groupByEmployee(List<Sale> sales, Long selectedEmployeeId) {
        Map<Long, MutableGroup> grouped = new TreeMap<>();
        for (Sale sale : sales) {
            for (SaleItem item : matchingItems(sale, selectedEmployeeId)) {
                Long employeeId = item.getEmployee().getId();
                MutableGroup group = grouped.computeIfAbsent(
                        employeeId,
                        id -> new MutableGroup(String.valueOf(id), item.getEmployeeNameSnapshot(), id));
                group.revenue = group.revenue.add(item.getNetLineTotal());
                group.saleIds.add(sale.getId());
                group.itemCount += item.getQuantity();
            }
        }
        return grouped.values().stream().map(MutableGroup::toResponse).toList();
    }

    private void validateEmployee(Long employeeId, Set<Long> salonIds) {
        if (employeeId == null) {
            return;
        }
        Employee employee = salonIds == null
                ? employeeRepository.findByIdAndActiveTrue(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Çalışan", employeeId))
                : employeeRepository.findByIdAndSalonIdInAndActiveTrue(employeeId, salonIds)
                        .orElseThrow(() -> new ResourceNotFoundException("Çalışan", employeeId));
        if (employee.getSalon() == null || !Boolean.TRUE.equals(employee.getSalon().getActive())) {
            throw new ResourceNotFoundException("Çalışan", employeeId);
        }
    }

    private List<SaleItem> matchingItems(Sale sale, Long employeeId) {
        if (employeeId == null) {
            return sale.getItems();
        }
        return sale.getItems().stream()
                .filter(item -> employeeId.equals(item.getEmployee().getId()))
                .toList();
    }

    private BigDecimal itemRevenue(Sale sale, Long employeeId) {
        return matchingItems(sale, employeeId).stream()
                .map(SaleItem::getNetLineTotal)
                .reduce(ZERO, BigDecimal::add);
    }

    private void validate(LocalDate from, LocalDate to, RevenueGroupBy groupBy) {
        if (from == null || to == null) {
            throw new BadRequestException("from ve to tarihleri zorunludur.");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("to tarihi from tarihinden önce olamaz.");
        }
        if (groupBy == null) {
            throw new BadRequestException("groupBy alanı zorunludur.");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static class MutableGroup {
        private final String key;
        private final String label;
        private final Long employeeId;
        private BigDecimal revenue = ZERO;
        private final Set<Long> saleIds = new HashSet<>();
        private long itemCount;

        private MutableGroup(String key, String label, Long employeeId) {
            this.key = key;
            this.label = label;
            this.employeeId = employeeId;
        }

        private RevenueGroupResponseDto toResponse() {
            return new RevenueGroupResponseDto(
                    key, label, employeeId, money(revenue), saleIds.size(), itemCount);
        }
    }
}
