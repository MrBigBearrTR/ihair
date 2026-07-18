package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.CompleteSaleRequestDto;
import com.bigbear.ihair.dto.request.SaleRequestDto;
import com.bigbear.ihair.dto.response.SaleResponseDto;
import com.bigbear.ihair.service.SaleService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class SaleControllerTest {

    @Test
    void createReturnsCreatedAndDelegatesToService() {
        SaleService service = mock(SaleService.class);
        SaleController controller = new SaleController(service);
        SaleRequestDto request = new SaleRequestDto();
        SaleResponseDto response = mock(SaleResponseDto.class);
        when(service.create(request)).thenReturn(response);

        var result = controller.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(service).create(request);
    }

    @Test
    void completeDelegatesToAtomicServiceOperation() {
        SaleService service = mock(SaleService.class);
        SaleController controller = new SaleController(service);
        CompleteSaleRequestDto request = new CompleteSaleRequestDto();
        SaleResponseDto response = mock(SaleResponseDto.class);
        when(service.complete(7L, request)).thenReturn(response);

        var result = controller.complete(7L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(service).complete(7L, request);
    }
}
