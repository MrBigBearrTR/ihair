package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.CampaignRequestDto;
import com.bigbear.ihair.dto.response.CampaignResponseDto;
import com.bigbear.ihair.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER')")
    public ResponseEntity<List<CampaignResponseDto>> getAll(
            @RequestParam(required = false) Long salonId) {
        return ResponseEntity.ok(campaignService.getAll(salonId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER')")
    public ResponseEntity<CampaignResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER', 'EMPLOYEE')")
    public ResponseEntity<CampaignResponseDto> validate(@RequestParam String code) {
        return ResponseEntity.ok(campaignService.validate(code));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER')")
    public ResponseEntity<CampaignResponseDto> create(@RequestBody CampaignRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER')")
    public ResponseEntity<CampaignResponseDto> update(@PathVariable Long id, @RequestBody CampaignRequestDto request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
