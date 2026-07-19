package com.bigbear.ihair.service;

import com.bigbear.ihair.config.CacheConfig;
import com.bigbear.ihair.entity.BrandAsset;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.enums.BrandAssetType;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.BrandAssetRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.security.SalonAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrandAssetService {
    private static final String GLOBAL_KEY = "global";
    private final BrandAssetRepository brandAssets;
    private final SalonRepository salons;
    private final SalonAccessService salonAccess;
    private final PngLogoValidator pngValidator;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    public Optional<BrandLogo> getSalonLogo(Long salonId) {
        salonAccess.requireSalonAccess(salonId);
        return requiredCache(CacheConfig.SALON_LOGOS).get(salonId, () -> brandAssets
                .findByTypeAndSalonId(BrandAssetType.SALON_LOGO, salonId).map(this::toLogo));
    }

    @Transactional(readOnly = true)
    public Optional<BrandLogo> getGlobalLogo() {
        return requiredCache(CacheConfig.GLOBAL_LOGO).get(GLOBAL_KEY, () -> brandAssets
                .findByTypeAndSalonIsNull(BrandAssetType.GLOBAL_APP_LOGO).map(this::toLogo));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void putSalonLogo(Long salonId, MultipartFile file) {
        ValidatedPng png = pngValidator.validate(file);
        Salon salon = salons.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", salonId));
        BrandAsset asset = brandAssets.findByTypeAndSalonId(BrandAssetType.SALON_LOGO, salonId)
                .orElseGet(BrandAsset::new);
        asset.setType(BrandAssetType.SALON_LOGO);
        asset.setSalon(salon);
        apply(asset, png);
        brandAssets.saveAndFlush(asset);
        evictAfterCommit(CacheConfig.SALON_LOGOS, salonId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void putGlobalLogo(MultipartFile file) {
        ValidatedPng png = pngValidator.validate(file);
        BrandAsset asset = brandAssets.findByTypeAndSalonIsNull(BrandAssetType.GLOBAL_APP_LOGO)
                .orElseGet(BrandAsset::new);
        asset.setType(BrandAssetType.GLOBAL_APP_LOGO);
        asset.setSalon(null);
        apply(asset, png);
        brandAssets.saveAndFlush(asset);
        evictAfterCommit(CacheConfig.GLOBAL_LOGO, GLOBAL_KEY);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteSalonLogo(Long salonId) {
        brandAssets.findByTypeAndSalonId(BrandAssetType.SALON_LOGO, salonId)
                .ifPresent(brandAssets::delete);
        brandAssets.flush();
        evictAfterCommit(CacheConfig.SALON_LOGOS, salonId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteGlobalLogo() {
        brandAssets.findByTypeAndSalonIsNull(BrandAssetType.GLOBAL_APP_LOGO)
                .ifPresent(brandAssets::delete);
        brandAssets.flush();
        evictAfterCommit(CacheConfig.GLOBAL_LOGO, GLOBAL_KEY);
    }

    private void apply(BrandAsset asset, ValidatedPng png) {
        asset.setContentType("image/png");
        asset.setData(png.data());
        asset.setContentLength(png.data().length);
        asset.setChecksum(png.checksum());
        asset.setWidth(png.width());
        asset.setHeight(png.height());
    }

    private BrandLogo toLogo(BrandAsset asset) {
        LocalDateTime modified = asset.getUpdatedAt() != null ? asset.getUpdatedAt() : asset.getCreatedAt();
        Instant lastModified = modified == null
                ? Instant.EPOCH : modified.atZone(ZoneId.systemDefault()).toInstant();
        return new BrandLogo(asset.getData(), asset.getContentType(), asset.getChecksum(), lastModified);
    }

    private Cache requiredCache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache == null) throw new IllegalStateException("Cache yapılandırması bulunamadı: " + name);
        return cache;
    }

    private void evictAfterCommit(String cacheName, Object key) {
        Cache cache = requiredCache(cacheName);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cache.evict(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cache.evict(key);
            }
        });
    }
}
