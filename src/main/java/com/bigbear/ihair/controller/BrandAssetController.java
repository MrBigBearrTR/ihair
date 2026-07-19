package com.bigbear.ihair.controller;

import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.service.BrandAssetService;
import com.bigbear.ihair.service.BrandLogo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class BrandAssetController {
    private final BrandAssetService brandAssetService;

    @GetMapping("/api/salons/{salonId}/logo")
    public ResponseEntity<byte[]> getSalonLogo(@PathVariable Long salonId, WebRequest request) {
        BrandLogo logo = brandAssetService.getSalonLogo(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon logosu bulunamadı."));
        return binaryResponse(logo, request, false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/api/salons/{salonId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> putSalonLogo(@PathVariable Long salonId,
                                             @RequestPart("file") MultipartFile file) {
        brandAssetService.putSalonLogo(salonId, file);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/salons/{salonId}/logo")
    public ResponseEntity<Void> deleteSalonLogo(@PathVariable Long salonId) {
        brandAssetService.deleteSalonLogo(salonId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/branding/logo")
    public ResponseEntity<byte[]> getGlobalLogo(WebRequest request) {
        BrandLogo logo = brandAssetService.getGlobalLogo()
                .orElseThrow(() -> new ResourceNotFoundException("Uygulama logosu bulunamadı."));
        return binaryResponse(logo, request, true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/api/branding/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> putGlobalLogo(@RequestPart("file") MultipartFile file) {
        brandAssetService.putGlobalLogo(file);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/branding/logo")
    public ResponseEntity<Void> deleteGlobalLogo() {
        brandAssetService.deleteGlobalLogo();
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> binaryResponse(BrandLogo logo, WebRequest request, boolean publicAsset) {
        long lastModified = logo.lastModified().toEpochMilli();
        if (request.checkNotModified(logo.etag(), lastModified)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        byte[] data = logo.data();
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(data.length)
                .eTag(logo.etag())
                .lastModified(lastModified)
                .cacheControl(publicAsset
                        ? CacheControl.maxAge(Duration.ofDays(1)).cachePublic().mustRevalidate()
                        : CacheControl.maxAge(Duration.ofHours(1)).cachePrivate().mustRevalidate());
        if (!publicAsset) response.header(HttpHeaders.VARY, HttpHeaders.AUTHORIZATION);
        return response.body(data);
    }
}
