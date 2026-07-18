package com.bigbear.ihair.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ErrorResponseDto {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String reason;
    private Map<String, Object> details;
    private String path;
}
