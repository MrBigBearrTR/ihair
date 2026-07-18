package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.HairService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HairServiceRepository extends JpaRepository<HairService, Long> {

    List<HairService> findAllByActiveTrue();

    List<HairService> findAllBySalonIdAndActiveTrue(Long salonId);

    List<HairService> findAllBySalonIdInAndActiveTrue(Iterable<Long> salonIds);
}
