package com.bigbear.ihair.repository;

import com.bigbear.ihair.entity.SalonSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalonSettingRepository extends JpaRepository<SalonSetting, Long> {

    List<SalonSetting> findAllBySalonId(Long salonId);

    Optional<SalonSetting> findBySalonIdAndSettingKey(Long salonId, String settingKey);

    boolean existsBySalonIdAndSettingKey(Long salonId, String settingKey);

    void deleteBySalonIdAndSettingKey(Long salonId, String settingKey);
}
