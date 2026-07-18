package com.bigbear.ihair.config;

import com.bigbear.ihair.controller.ReportController;
import com.bigbear.ihair.controller.SaleController;
import com.bigbear.ihair.entity.enums.RevenueGroupBy;
import com.bigbear.ihair.service.ReportService;
import com.bigbear.ihair.service.SaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SalesAuthorizationTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        authenticate("EMPLOYEE");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void employeeCanUseSalesButCannotReadRevenueReport() {
        SaleController sales = context.getBean(SaleController.class);
        ReportController reports = context.getBean(ReportController.class);

        assertDoesNotThrow(() -> sales.getAll(null, null));
        assertThrows(AccessDeniedException.class, () -> reports.getRevenue(
                null, LocalDate.now(), LocalDate.now(), RevenueGroupBy.DAY, null));
    }

    @Test
    void salonOwnerCanUseSalesAndRevenueReport() {
        authenticate("SALON_OWNER");
        SaleController sales = context.getBean(SaleController.class);
        ReportController reports = context.getBean(ReportController.class);

        assertDoesNotThrow(() -> sales.getAll(null, null));
        assertDoesNotThrow(() -> reports.getRevenue(
                1L, LocalDate.now(), LocalDate.now(), RevenueGroupBy.DAY, null));
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test", "password", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean SaleService saleService() {
            return mock(SaleService.class);
        }

        @Bean ReportService reportService() {
            return mock(ReportService.class);
        }

        @Bean SaleController saleController(SaleService service) {
            return new SaleController(service);
        }

        @Bean ReportController reportController(ReportService service) {
            return new ReportController(service);
        }
    }
}
