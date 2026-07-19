package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.CampaignRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRedemptionRepository extends JpaRepository<CampaignRedemption, Long> {
    boolean existsBySaleId(Long saleId);
}
