package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findAllByActiveTrue();

    Optional<Campaign> findByCode(String code);

    boolean existsByCode(String code);
}
