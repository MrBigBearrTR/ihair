package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.AppointmentStatusRequestDto;
import com.bigbear.ihair.dto.response.AppointmentResponseDto;
import com.bigbear.ihair.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentControllerTest {

    @Test
    void patchStatusDelegatesToStatusServiceMethod() {
        AppointmentService service = mock(AppointmentService.class);
        AppointmentController controller = new AppointmentController(service);
        AppointmentStatusRequestDto request = mock(AppointmentStatusRequestDto.class);
        AppointmentResponseDto response = mock(AppointmentResponseDto.class);
        when(service.updateStatus(42L, request)).thenReturn(response);

        ResponseEntity<AppointmentResponseDto> result = controller.updateStatus(42L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(service).updateStatus(42L, request);
    }
}
