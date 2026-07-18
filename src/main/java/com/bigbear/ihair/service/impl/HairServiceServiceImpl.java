package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.HairServiceRequestDto;
import com.bigbear.ihair.dto.response.HairServiceResponseDto;
import com.bigbear.ihair.entity.HairService;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.HairServiceRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.HairServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HairServiceServiceImpl implements HairServiceService {

    private final HairServiceRepository hairServiceRepository;
    private final SalonRepository salonRepository;
    private final SalonAccessService salonAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<HairServiceResponseDto> getAll(Long salonId) {
        Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
        List<HairService> services = salonIds == null
                ? hairServiceRepository.findAllByActiveTrue()
                : hairServiceRepository.findAllBySalonIdInAndActiveTrue(salonIds);
        return services.stream().map(HairServiceResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HairServiceResponseDto getById(Long id) {
        HairService service = findActiveById(id);
        salonAccessService.requireSalonAccess(service.getSalon().getId());
        return new HairServiceResponseDto(service);
    }

    @Override
    @Transactional
    public HairServiceResponseDto create(HairServiceRequestDto request) {
        Salon salon = findActiveSalon(salonAccessService.resolveSalonId(request.getSalonId()));
        HairService service = new HairService();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setDurationMinutes(requirePositiveDuration(request.getDurationMinutes()));
        service.setSalon(salon);
        return new HairServiceResponseDto(hairServiceRepository.save(service));
    }

    @Override
    @Transactional
    public HairServiceResponseDto update(Long id, HairServiceRequestDto request) {
        HairService service = findActiveById(id);
        salonAccessService.requireSalonAccess(service.getSalon().getId());
        Salon salon = request.getSalonId() == null
                ? service.getSalon()
                : findActiveSalon(salonAccessService.resolveSalonId(request.getSalonId()));
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setDurationMinutes(requirePositiveDuration(request.getDurationMinutes()));
        service.setSalon(salon);
        return new HairServiceResponseDto(hairServiceRepository.save(service));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HairService service = findActiveById(id);
        salonAccessService.requireSalonAccess(service.getSalon().getId());
        service.setActive(false);
        hairServiceRepository.save(service);
    }

    private HairService findActiveById(Long id) {
        HairService service = hairServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hizmet", id));
        if (Boolean.FALSE.equals(service.getActive())) {
            throw new ResourceNotFoundException("Hizmet", id);
        }
        return service;
    }

    private Salon findActiveSalon(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", salonId));
        if (Boolean.FALSE.equals(salon.getActive())) {
            throw new ResourceNotFoundException("Salon", salonId);
        }
        return salon;
    }

    private int requirePositiveDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BadRequestException("Hizmet süresi pozitif bir dakika değeri olmalıdır.");
        }
        return durationMinutes;
    }
}
