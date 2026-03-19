package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.SalonRequestDto;
import com.bigbear.ihair.dto.response.SalonResponseDto;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.service.SalonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalonServiceImpl implements SalonService {

    private final SalonRepository salonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalonResponseDto> getAll() {
        return salonRepository.findAllByActiveTrue().stream()
                .map(SalonResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalonResponseDto getById(Long id) {
        return new SalonResponseDto(findActiveById(id));
    }

    @Override
    @Transactional
    public SalonResponseDto create(SalonRequestDto request) {
        Salon salon = new Salon();
        salon.setName(request.getName());
        salon.setAddress(request.getAddress());
        salon.setPhone(request.getPhone());
        salon.setEmail(request.getEmail());
        return new SalonResponseDto(salonRepository.save(salon));
    }

    @Override
    @Transactional
    public SalonResponseDto update(Long id, SalonRequestDto request) {
        Salon salon = findActiveById(id);
        salon.setName(request.getName());
        salon.setAddress(request.getAddress());
        salon.setPhone(request.getPhone());
        salon.setEmail(request.getEmail());
        return new SalonResponseDto(salonRepository.save(salon));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Salon salon = findActiveById(id);
        salon.setActive(false);
        salonRepository.save(salon);
    }

    private Salon findActiveById(Long id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", id));
        if (Boolean.FALSE.equals(salon.getActive())) {
            throw new ResourceNotFoundException("Salon", id);
        }
        return salon;
    }
}
