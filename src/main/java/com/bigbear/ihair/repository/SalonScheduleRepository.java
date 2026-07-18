package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.SalonSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonScheduleRepository extends JpaRepository<SalonSchedule, Long> {

    @EntityGraph(attributePaths = "days")
    Optional<SalonSchedule> findBySalonId(Long salonId);
}
