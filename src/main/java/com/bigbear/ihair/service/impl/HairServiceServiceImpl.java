package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.HairServiceRequestDto;
import com.bigbear.ihair.dto.response.HairServiceResponseDto;
import com.bigbear.ihair.entity.HairService;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.HairServiceRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.service.HairServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HairServiceServiceImpl implements HairServiceService {

    private final HairServiceRepository hairServiceRepository;
    private final SalonRepository salonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HairServiceResponseDto> getAll(Long salonId) {
        List<HairService> services = salonId != null
                ? hairServiceRepository.findAllBySalonIdAndActiveTrue(salonId)
                : hairServiceRepository.findAllByActiveTrue();
        return services.stream().map(HairServiceResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HairServiceResponseDto getById(Long id) {
        return new HairServiceResponseDto(findActiveById(id));
    }

    @Override
    @Transactional
    public HairServiceResponseDto create(HairServiceRequestDto request) {
        Salon salon = findActiveSalon(request.getSalonId());
        HairService service = new HairService();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setSalon(salon);
        return new HairServiceResponseDto(hairServiceRepository.save(service));
    }

    @Override
    @Transactional
    public HairServiceResponseDto update(Long id, HairServiceRequestDto request) {
        HairService service = findActiveById(id);
        Salon salon = findActiveSalon(request.getSalonId());
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setSalon(salon);
        return new HairServiceResponseDto(hairServiceRepository.save(service));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HairService service = findActiveById(id);
        service.setActive(false);
        hairServiceRepository.save(service);
    }

    private HairService findActiveById(Long id) {
        HairService service = hairServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HairService", id));
        if (Boolean.FALSE.equals(service.getActive())) {
            throw new ResourceNotFoundException("HairService", id);
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
}
