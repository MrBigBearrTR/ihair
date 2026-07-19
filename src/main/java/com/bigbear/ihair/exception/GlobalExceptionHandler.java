package com.bigbear.ihair.exception;

import com.bigbear.ihair.dto.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleAppointmentConflict(AppointmentConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "APPOINTMENT_CONFLICT", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OutsideWorkingHoursException.class)
    public ResponseEntity<ErrorResponseDto> handleOutsideWorkingHours(
            OutsideWorkingHoursException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponseDto.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error(HttpStatus.CONFLICT.getReasonPhrase())
                        .code("OUTSIDE_WORKING_HOURS_CONFIRMATION_REQUIRED")
                        .message(ex.getMessage())
                        .reason(ex.getReason())
                        .details(ex.getDetails())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    public ResponseEntity<ErrorResponseDto> handlePayloadTooLarge(
            PayloadTooLargeException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "Logo dosyası en fazla 1 MB olabilir.", request.getRequestURI());
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponseDto> handleMultipart(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_MULTIPART_REQUEST",
                "Logo dosyası multipart formunda 'file' alanıyla gönderilmelidir.",
                request.getRequestURI());
    }

    @ExceptionHandler(SalonScopeException.class)
    public ResponseEntity<ErrorResponseDto> handleSalonScope(SalonScopeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Kullanıcı adı veya şifre hatalı.", request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        log.error("Beklenmeyen API hatası: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "İşlem sırasında beklenmeyen bir hata oluştu.", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status).body(
                ErrorResponseDto.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .code(code)
                        .message(message)
                        .path(path)
                        .build()
        );
    }
}
