package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.SalonSettingRequestDto;
import com.bigbear.ihair.dto.response.SalonSettingResponseDto;
import com.bigbear.ihair.service.SalonSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salons/{salonId}/settings")
@RequiredArgsConstructor
public class SalonSettingController {

    private final SalonSettingService salonSettingService;

    @GetMapping
    public ResponseEntity<List<SalonSettingResponseDto>> getAll(@PathVariable Long salonId) {
        return ResponseEntity.ok(salonSettingService.getAllBySalon(salonId));
    }

    @GetMapping("/{key}")
    public ResponseEntity<SalonSettingResponseDto> getByKey(
            @PathVariable Long salonId,
            @PathVariable String key) {
        return ResponseEntity.ok(salonSettingService.getByKey(salonId, key));
    }

    @PutMapping("/{key}")
    public ResponseEntity<SalonSettingResponseDto> upsert(
            @PathVariable Long salonId,
            @PathVariable String key,
            @RequestBody SalonSettingRequestDto request) {
        return ResponseEntity.ok(salonSettingService.upsert(salonId, key, request));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(
            @PathVariable Long salonId,
            @PathVariable String key) {
        salonSettingService.delete(salonId, key);
        return ResponseEntity.noContent().build();
    }
}
