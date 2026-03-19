package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.HairServiceRequestDto;
import com.bigbear.ihair.dto.response.HairServiceResponseDto;
import com.bigbear.ihair.service.HairServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hair-services")
@RequiredArgsConstructor
public class HairServiceController {

    private final HairServiceService hairServiceService;

    @GetMapping
    public ResponseEntity<List<HairServiceResponseDto>> getAll(@RequestParam(required = false) Long salonId) {
        return ResponseEntity.ok(hairServiceService.getAll(salonId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HairServiceResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(hairServiceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<HairServiceResponseDto> create(@RequestBody HairServiceRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hairServiceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HairServiceResponseDto> update(@PathVariable Long id, @RequestBody HairServiceRequestDto request) {
        return ResponseEntity.ok(hairServiceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hairServiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
