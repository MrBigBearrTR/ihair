package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.BrandAsset;
import com.bigbear.ihair.entity.enums.BrandAssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandAssetRepository extends JpaRepository<BrandAsset, Long> {
    Optional<BrandAsset> findByTypeAndSalonId(BrandAssetType type, Long salonId);
    Optional<BrandAsset> findByTypeAndSalonIsNull(BrandAssetType type);
}
