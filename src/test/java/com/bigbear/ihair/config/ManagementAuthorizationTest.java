package com.bigbear.ihair.config;

import com.bigbear.ihair.controller.CampaignController;
import com.bigbear.ihair.controller.EmployeeController;
import com.bigbear.ihair.controller.HairServiceController;
import com.bigbear.ihair.dto.request.CampaignRequestDto;
import com.bigbear.ihair.dto.request.EmployeeRequestDto;
import com.bigbear.ihair.dto.request.HairServiceRequestDto;
import com.bigbear.ihair.service.CampaignService;
import com.bigbear.ihair.service.EmployeeService;
import com.bigbear.ihair.service.HairServiceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ManagementAuthorizationTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "employee", "password", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void employeeCanReadEmployeesButCannotManageThem() {
        EmployeeController controller = context.getBean(EmployeeController.class);

        assertDoesNotThrow(() -> controller.getAll(null));
        assertThrows(AccessDeniedException.class, () -> controller.create(mock(EmployeeRequestDto.class)));
    }

    @Test
    void employeeCanReadHairServicesButCannotManageThem() {
        HairServiceController controller = context.getBean(HairServiceController.class);

        assertDoesNotThrow(() -> controller.getAll(null));
        assertThrows(AccessDeniedException.class, () -> controller.create(mock(HairServiceRequestDto.class)));
    }

    @Test
    void employeeCanOnlyValidateCampaigns() {
        CampaignController controller = context.getBean(CampaignController.class);

        assertDoesNotThrow(() -> controller.validate("TEST"));
        assertThrows(AccessDeniedException.class, () -> controller.getAll(null));
        assertThrows(AccessDeniedException.class, () -> controller.create(mock(CampaignRequestDto.class)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestConfig {
        @Bean EmployeeService employeeService() {
            return mock(EmployeeService.class);
        }

        @Bean HairServiceService hairServiceService() {
            return mock(HairServiceService.class);
        }

        @Bean CampaignService campaignService() {
            return mock(CampaignService.class);
        }

        @Bean EmployeeController employeeController(EmployeeService service) {
            return new EmployeeController(service);
        }

        @Bean HairServiceController hairServiceController(HairServiceService service) {
            return new HairServiceController(service);
        }

        @Bean CampaignController campaignController(CampaignService service) {
            return new CampaignController(service);
        }
    }
}
