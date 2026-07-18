package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.SalonSettingRequestDto;
import com.bigbear.ihair.dto.response.SalonSettingResponseDto;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.entity.SalonSetting;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.repository.SalonSettingRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.SalonSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalonSettingServiceImpl implements SalonSettingService {

    private final SalonSettingRepository salonSettingRepository;
    private final SalonRepository salonRepository;
    private final SalonAccessService salonAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<SalonSettingResponseDto> getAllBySalon(Long salonId) {
        salonAccessService.requireSalonAccess(salonId);
        findActiveSalon(salonId);
        return salonSettingRepository.findAllBySalonId(salonId)
                .stream().map(SalonSettingResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalonSettingResponseDto getByKey(Long salonId, String key) {
        salonAccessService.requireSalonAccess(salonId);
        findActiveSalon(salonId);
        SalonSetting setting = salonSettingRepository
                .findBySalonIdAndSettingKey(salonId, normalize(key))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ayar bulunamadı. Salon: " + salonId + ", Key: " + key));
        return new SalonSettingResponseDto(setting);
    }

    @Override
    @Transactional
    public SalonSettingResponseDto upsert(Long salonId, String key, SalonSettingRequestDto request) {
        salonAccessService.requireSalonAccess(salonId);
        Salon salon = findActiveSalon(salonId);
        String normalizedKey = normalize(key);

        SalonSetting setting = salonSettingRepository
                .findBySalonIdAndSettingKey(salonId, normalizedKey)
                .orElse(new SalonSetting());

        setting.setSalon(salon);
        setting.setSettingKey(normalizedKey);
        setting.setSettingType(request.getSettingType());
        setting.setSettingValue(request.getSettingValue());
        setting.setDescription(request.getDescription());

        return new SalonSettingResponseDto(salonSettingRepository.save(setting));
    }

    @Override
    @Transactional
    public void delete(Long salonId, String key) {
        salonAccessService.requireSalonAccess(salonId);
        findActiveSalon(salonId);
        String normalizedKey = normalize(key);
        if (!salonSettingRepository.existsBySalonIdAndSettingKey(salonId, normalizedKey)) {
            throw new ResourceNotFoundException(
                    "Ayar bulunamadı. Salon: " + salonId + ", Key: " + normalizedKey);
        }
        salonSettingRepository.deleteBySalonIdAndSettingKey(salonId, normalizedKey);
    }

    private Salon findActiveSalon(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", salonId));
        if (Boolean.FALSE.equals(salon.getActive())) {
            throw new ResourceNotFoundException("Salon", salonId);
        }
        return salon;
    }

    private String normalize(String key) {
        return key == null ? null : key.toUpperCase().trim();
    }
}
