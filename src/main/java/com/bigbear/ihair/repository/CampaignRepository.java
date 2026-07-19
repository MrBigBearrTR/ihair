package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.Campaign;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findAllByActiveTrue();

    List<Campaign> findAllBySalonIdAndActiveTrue(Long salonId);

    List<Campaign> findAllBySalonIdInAndActiveTrue(Iterable<Long> salonIds);

    Optional<Campaign> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Campaign c where c.id = :id")
    Optional<Campaign> findWithLockById(@Param("id") Long id);
}
