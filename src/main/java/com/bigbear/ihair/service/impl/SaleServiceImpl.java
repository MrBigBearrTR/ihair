package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.CompleteSaleRequestDto;
import com.bigbear.ihair.dto.request.SaleItemRequestDto;
import com.bigbear.ihair.dto.request.SalePaymentRequestDto;
import com.bigbear.ihair.dto.request.SaleRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;
import com.bigbear.ihair.dto.response.SaleResponseDto;
import com.bigbear.ihair.entity.*;
import com.bigbear.ihair.entity.enums.AppointmentStatus;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.entity.enums.SaleStatus;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.DuplicateResourceException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.*;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private static final int MAX_QUANTITY = 100;
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final SaleRepository saleRepository;
    private final SalonRepository salonRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final HairServiceRepository hairServiceRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonAccessService salonAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getAll(Long salonId, SaleStatus status) {
        return loadSales(salonId, status).stream().map(SaleResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getAll(
            Long salonId,
            SaleStatus status,
            LocalDate from,
            LocalDate to,
            Long employeeId) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("to tarihi from tarihinden önce olamaz.");
        }
        List<Sale> sales = loadSales(salonId, status);
        if (employeeId != null) {
            Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
            if (salonIds == null) {
                employeeRepository.findByIdAndActiveTrue(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Çalışan", employeeId));
            } else {
                employeeRepository.findByIdAndSalonIdInAndActiveTrue(employeeId, salonIds)
                        .orElseThrow(() -> new ResourceNotFoundException("Çalışan", employeeId));
            }
        }
        return sales.stream()
                .filter(sale -> from == null
                        || (sale.getCompletedAt() != null
                        && !sale.getCompletedAt().toLocalDate().isBefore(from)))
                .filter(sale -> to == null
                        || (sale.getCompletedAt() != null
                        && !sale.getCompletedAt().toLocalDate().isAfter(to)))
                .filter(sale -> employeeId == null || sale.getItems().stream()
                        .anyMatch(item -> employeeId.equals(item.getEmployee().getId())))
                .map(SaleResponseDto::new)
                .toList();
    }

    private List<Sale> loadSales(Long salonId, SaleStatus status) {
        Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
        List<Sale> sales;
        if (salonIds == null) {
            sales = status == null
                    ? saleRepository.findAllByOrderByCreatedAtDesc()
                    : saleRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else {
            sales = status == null
                    ? saleRepository.findAllBySalonIdInOrderByCreatedAtDesc(salonIds)
                    : saleRepository.findAllBySalonIdInAndStatusOrderByCreatedAtDesc(salonIds, status);
        }
        return sales;
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponseDto getById(Long id) {
        Sale sale = findById(id);
        requireAccess(sale);
        return new SaleResponseDto(sale);
    }

    @Override
    @Transactional
    public SaleResponseDto create(SaleRequestDto request) {
        requireRequest(request);
        Long salonId = salonAccessService.resolveSalonId(request.getSalonId());
        Salon salon = findActiveSalon(salonId);
        Customer customer = findActiveCustomer(request.getCustomerId(), salonId);
        User currentUser = salonAccessService.currentUser();

        if (request.getSourceAppointmentId() != null) {
            Sale existingSale = saleRepository
                    .findBySourceAppointmentId(request.getSourceAppointmentId())
                    .orElse(null);
            if (existingSale != null) {
                requireAccess(existingSale);
                requireOpen(existingSale);
                if (!existingSale.getSalon().getId().equals(salonId)) {
                    throw new BadRequestException("Randevu satışı farklı bir salona aittir.");
                }
                if (!existingSale.getSourceAppointment().getCustomer().getId().equals(customer.getId())) {
                    throw new BadRequestException("Randevu ve satış aynı müşteriye ait olmalıdır.");
                }
                existingSale.setCustomer(customer);
                existingSale.setNotes(normalizeNotes(request.getNotes()));
                existingSale.getItems().clear();
                saleRepository.saveAndFlush(existingSale);
                replaceItems(existingSale, request.getItems(), currentUser);
                calculateTotals(existingSale);
                return new SaleResponseDto(saleRepository.saveAndFlush(existingSale));
            }
        }

        Sale sale = new Sale();
        sale.setSalon(salon);
        sale.setCustomer(customer);
        sale.setCreatedBy(currentUser);
        sale.setStatus(SaleStatus.OPEN);
        sale.setNotes(normalizeNotes(request.getNotes()));
        applySourceAppointment(sale, request.getSourceAppointmentId(), null);
        replaceItems(sale, request.getItems(), currentUser);
        calculateTotals(sale);

        try {
            return new SaleResponseDto(saleRepository.saveAndFlush(sale));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Bu randevu başka bir satışa bağlanmış.");
        }
    }

    @Override
    @Transactional
    public SaleResponseDto update(Long id, SaleRequestDto request) {
        requireRequest(request);
        Sale sale = findWithLock(id);
        requireAccess(sale);
        requireOpen(sale);

        Long requestedSalonId = request.getSalonId() == null ? sale.getSalon().getId() : request.getSalonId();
        Long salonId = salonAccessService.resolveSalonId(requestedSalonId);
        if (!sale.getSalon().getId().equals(salonId)) {
            throw new BadRequestException("Açık satışın salonu değiştirilemez.");
        }
        Customer customer = findActiveCustomer(request.getCustomerId(), salonId);
        sale.setCustomer(customer);
        sale.setNotes(normalizeNotes(request.getNotes()));
        applySourceAppointment(sale, request.getSourceAppointmentId(), sale.getId());
        sale.getItems().clear();
        // Orphan satırları yeni satırlar aynı (sale_id, position) değerleriyle
        // eklenmeden önce sil; aksi halde PostgreSQL unique kısıtı tetiklenir.
        saleRepository.saveAndFlush(sale);
        replaceItems(sale, request.getItems(), salonAccessService.currentUser());
        calculateTotals(sale);

        try {
            return new SaleResponseDto(saleRepository.saveAndFlush(sale));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Bu randevu başka bir satışa bağlanmış.");
        }
    }

    @Override
    @Transactional
    public SaleResponseDto complete(Long id, CompleteSaleRequestDto request) {
        Sale sale = findWithLock(id);
        requireAccess(sale);
        if (sale.getStatus() == SaleStatus.COMPLETED) {
            completeSourceAppointment(sale);
            return new SaleResponseDto(sale);
        }
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw new BadRequestException("İptal edilmiş satış tamamlanamaz.");
        }
        if (sale.getItems().isEmpty()) {
            throw new BadRequestException("Ürünsüz veya hizmetsiz satış tamamlanamaz.");
        }
        SalePaymentRequestDto paymentRequest = requireSinglePayment(request);
        calculateTotals(sale);
        BigDecimal total = money(sale.getTotalAmount());
        if (paymentRequest.getAmount() != null
                && money(paymentRequest.getAmount()).compareTo(total) != 0) {
            throw new BadRequestException("Ödeme tutarı satış toplamı ile eşleşmelidir.");
        }

        SalePayment payment = new SalePayment();
        payment.setMethod(paymentRequest.getMethod());
        payment.setAmount(total);
        sale.addPayment(payment);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setCompletedAt(LocalDateTime.now());
        sale.setCancelledAt(null);
        completeSourceAppointment(sale);
        return new SaleResponseDto(saleRepository.saveAndFlush(sale));
    }

    @Override
    @Transactional
    public SaleResponseDto cancel(Long id) {
        Sale sale = findWithLock(id);
        requireAccess(sale);
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            return new SaleResponseDto(sale);
        }
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw new BadRequestException("Tamamlanmış satış iptal edilemez.");
        }
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancelledAt(LocalDateTime.now());
        sale.setCompletedAt(null);
        sale.getPayments().clear();
        return new SaleResponseDto(saleRepository.saveAndFlush(sale));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAvailableAppointments(Long salonId) {
        Long resolvedSalonId = salonAccessService.resolveSalonId(salonId);
        findActiveSalon(resolvedSalonId);
        return appointmentRepository.findAvailableForSale(
                        resolvedSalonId,
                        List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED))
                .stream().map(AppointmentResponseDto::new).toList();
    }

    private void completeSourceAppointment(Sale sale) {
        Appointment appointment = sale.getSourceAppointment();
        if (appointment == null || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            return;
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }

    private void applySourceAppointment(Sale sale, Long appointmentId, Long currentSaleId) {
        if (appointmentId == null) {
            sale.setSourceAppointment(null);
            return;
        }
        boolean alreadyUsed = currentSaleId == null
                ? saleRepository.existsBySourceAppointmentId(appointmentId)
                : saleRepository.existsBySourceAppointmentIdAndIdNot(appointmentId, currentSaleId);
        if (alreadyUsed) {
            throw new DuplicateResourceException("Bu randevu başka bir satışa bağlanmış.");
        }
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Randevu", appointmentId));
        Long salonId = sale.getSalon().getId();
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException(
                    "Yalnızca onaylanmış veya tamamlanmış randevular satışa aktarılabilir.");
        }
        if (!salonId.equals(appointment.getEmployee().getSalon().getId())) {
            throw new BadRequestException("Randevu satışın salonuna ait değildir.");
        }
        if (!sale.getCustomer().getId().equals(appointment.getCustomer().getId())) {
            throw new BadRequestException("Randevu ve satış aynı müşteriye ait olmalıdır.");
        }
        requireActiveEmployee(appointment.getEmployee(), salonId);
        requireActiveService(appointment.getHairService(), salonId);
        if (appointment.getFinalPrice() == null) {
            throw new BadRequestException("Randevunun nihai fiyatı bulunmuyor.");
        }
        sale.setSourceAppointment(appointment);
    }

    private void replaceItems(Sale sale, List<SaleItemRequestDto> requests, User currentUser) {
        Set<Integer> positions = new HashSet<>();
        int nextPosition = 0;
        if (sale.getSourceAppointment() != null) {
            Appointment appointment = sale.getSourceAppointment();
            SaleItem imported = createItem(
                    sale,
                    appointment.getHairService(),
                    appointment.getEmployee(),
                    1,
                    nextPosition++,
                    appointment.getFinalPrice());
            sale.addItem(imported);
            positions.add(imported.getPosition());
        }

        List<SaleItemRequestDto> safeRequests = requests == null ? List.of() : requests;
        for (SaleItemRequestDto request : safeRequests) {
            if (request == null || request.getServiceId() == null) {
                throw new BadRequestException("Her satış satırında serviceId zorunludur.");
            }
            int quantity = requireQuantity(request.getQuantity());
            int position = request.getPosition() == null ? nextPosition : request.getPosition();
            if (position < 0 || !positions.add(position)) {
                throw new BadRequestException("Satış satırı position değerleri benzersiz ve negatif olmayan olmalıdır.");
            }
            nextPosition = Math.max(nextPosition, position + 1);
            HairService service = findActiveService(request.getServiceId(), sale.getSalon().getId());
            Employee employee = resolveEmployee(request.getEmployeeId(), currentUser, sale.getSalon().getId());
            sale.addItem(createItem(sale, service, employee, quantity, position, service.getPrice()));
        }
        if (sale.getItems().isEmpty()) {
            throw new BadRequestException("Satışta en az bir hizmet satırı bulunmalıdır.");
        }
    }

    private SaleItem createItem(
            Sale sale,
            HairService service,
            Employee employee,
            int quantity,
            int position,
            BigDecimal unitPrice) {
        BigDecimal safeUnitPrice = requirePrice(unitPrice, "Birim fiyat");
        BigDecimal listPrice = requirePrice(service.getPrice(), "Hizmet liste fiyatı");
        SaleItem item = new SaleItem();
        item.setSale(sale);
        item.setService(service);
        item.setEmployee(employee);
        item.setQuantity(quantity);
        item.setPosition(position);
        item.setUnitPrice(safeUnitPrice);
        item.setListPrice(listPrice);
        item.setLineTotal(money(safeUnitPrice.multiply(BigDecimal.valueOf(quantity))));
        item.setServiceNameSnapshot(service.getName());
        item.setEmployeeNameSnapshot(fullName(employee.getFirstName(), employee.getLastName()));
        return item;
    }

    private Employee resolveEmployee(Long employeeId, User currentUser, Long salonId) {
        Long resolvedEmployeeId = employeeId;
        if (currentUser.getRole() == Role.EMPLOYEE && resolvedEmployeeId == null) {
            if (currentUser.getEmployee() == null) {
                throw new BadRequestException("Çalışan kullanıcının çalışan kaydı bulunmuyor.");
            }
            resolvedEmployeeId = currentUser.getEmployee().getId();
        }
        if (resolvedEmployeeId == null) {
            throw new BadRequestException("ADMIN ve SALON_OWNER kullanıcıları için employeeId zorunludur.");
        }
        return findActiveEmployee(resolvedEmployeeId, salonId);
    }

    private void calculateTotals(Sale sale) {
        BigDecimal subtotal = sale.getItems().stream()
                .map(SaleItem::getLineTotal)
                .reduce(ZERO, BigDecimal::add);
        sale.setSubtotal(money(subtotal));
        sale.setTotalAmount(money(subtotal));
    }

    private SalePaymentRequestDto requireSinglePayment(CompleteSaleRequestDto request) {
        if (request == null || request.getPayments() == null || request.getPayments().size() != 1) {
            throw new BadRequestException("Satışı tamamlamak için tam olarak bir ödeme gönderilmelidir.");
        }
        SalePaymentRequestDto payment = request.getPayments().getFirst();
        if (payment == null || payment.getMethod() == null) {
            throw new BadRequestException("Ödeme yöntemi zorunludur.");
        }
        if (payment.getAmount() != null && payment.getAmount().signum() < 0) {
            throw new BadRequestException("Ödeme tutarı negatif olamaz.");
        }
        return payment;
    }

    private Sale findById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Satış", id));
    }

    private Sale findWithLock(Long id) {
        return saleRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Satış", id));
    }

    private Salon findActiveSalon(Long id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", id));
        if (!Boolean.TRUE.equals(salon.getActive())) {
            throw new ResourceNotFoundException("Salon", id);
        }
        return salon;
    }

    private Customer findActiveCustomer(Long id, Long salonId) {
        if (id == null) {
            throw new BadRequestException("customerId alanı zorunludur.");
        }
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Müşteri", id));
        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new ResourceNotFoundException("Müşteri", id);
        }
        if (customer.getSalon() == null || !salonId.equals(customer.getSalon().getId())) {
            throw new BadRequestException("Müşteri satışın salonuna ait değildir.");
        }
        return customer;
    }

    private HairService findActiveService(Long id, Long salonId) {
        HairService service = hairServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hizmet", id));
        requireActiveService(service, salonId);
        return service;
    }

    private Employee findActiveEmployee(Long id, Long salonId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Çalışan", id));
        requireActiveEmployee(employee, salonId);
        return employee;
    }

    private void requireActiveService(HairService service, Long salonId) {
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new ResourceNotFoundException("Hizmet", service.getId());
        }
        if (service.getSalon() == null || !salonId.equals(service.getSalon().getId())) {
            throw new BadRequestException("Hizmet satışın salonuna ait değildir.");
        }
    }

    private void requireActiveEmployee(Employee employee, Long salonId) {
        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new ResourceNotFoundException("Çalışan", employee.getId());
        }
        if (employee.getSalon() == null || !salonId.equals(employee.getSalon().getId())) {
            throw new BadRequestException("Çalışan satışın salonuna ait değildir.");
        }
    }

    private void requireAccess(Sale sale) {
        salonAccessService.requireSalonAccess(sale.getSalon().getId());
    }

    private void requireOpen(Sale sale) {
        if (sale.getStatus() != SaleStatus.OPEN) {
            throw new BadRequestException("Yalnızca açık satışlar güncellenebilir.");
        }
    }

    private void requireRequest(SaleRequestDto request) {
        if (request == null) {
            throw new BadRequestException("Satış isteği zorunludur.");
        }
    }

    private int requireQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0 || quantity > MAX_QUANTITY) {
            throw new BadRequestException("quantity 1 ile " + MAX_QUANTITY + " arasında olmalıdır.");
        }
        return quantity;
    }

    private BigDecimal requirePrice(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new BadRequestException(fieldName + " negatif olamaz ve boş bırakılamaz.");
        }
        return money(value);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }

    private String fullName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName)).trim();
    }
}
