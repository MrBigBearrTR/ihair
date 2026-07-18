package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.SalonHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SalonHolidayRepository extends JpaRepository<SalonHoliday, Long> {
    List<SalonHoliday> findAllBySalonIdOrderByStartDateAsc(Long salonId);
    boolean existsBySalonIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long salonId, LocalDate endDate, LocalDate startDate);
}
