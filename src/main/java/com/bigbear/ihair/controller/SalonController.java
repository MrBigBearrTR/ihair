package com.bigbear.ihair.controller;

import com.bigbear.ihair.dto.request.SalonRequestDto;
import com.bigbear.ihair.dto.response.SalonResponseDto;
import com.bigbear.ihair.service.SalonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salons")
@RequiredArgsConstructor
public class SalonController {

    private final SalonService salonService;

    @GetMapping
    public ResponseEntity<List<SalonResponseDto>> getAll() {
        return ResponseEntity.ok(salonService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalonResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salonService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SalonResponseDto> create(@RequestBody SalonRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salonService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalonResponseDto> update(@PathVariable Long id, @RequestBody SalonRequestDto request) {
        return ResponseEntity.ok(salonService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
