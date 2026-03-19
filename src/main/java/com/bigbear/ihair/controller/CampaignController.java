package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.CampaignRequestDto;
import com.bigbear.ihair.dto.response.CampaignResponseDto;
import com.bigbear.ihair.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    public ResponseEntity<List<CampaignResponseDto>> getAll() {
        return ResponseEntity.ok(campaignService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/validate")
    public ResponseEntity<CampaignResponseDto> validate(@RequestParam String code) {
        return ResponseEntity.ok(campaignService.validate(code));
    }

    @PostMapping
    public ResponseEntity<CampaignResponseDto> create(@RequestBody CampaignRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponseDto> update(@PathVariable Long id, @RequestBody CampaignRequestDto request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
