package com.bigbear.ihair.service.impl;

import com.bigbear.ihair.dto.request.CampaignRequestDto;
import com.bigbear.ihair.dto.response.CampaignResponseDto;
import com.bigbear.ihair.entity.Campaign;
import com.bigbear.ihair.entity.Customer;
import com.bigbear.ihair.entity.Salon;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.DuplicateResourceException;
import com.bigbear.ihair.exception.ResourceNotFoundException;
import com.bigbear.ihair.repository.CampaignRepository;
import com.bigbear.ihair.repository.CustomerRepository;
import com.bigbear.ihair.repository.SalonRepository;
import com.bigbear.ihair.security.SalonAccessService;
import com.bigbear.ihair.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final CustomerRepository customerRepository;
    private final SalonRepository salonRepository;
    private final SalonAccessService salonAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponseDto> getAll(Long salonId) {
        Set<Long> salonIds = salonAccessService.resolveSalonIdsForList(salonId);
        List<Campaign> campaigns = salonIds == null
                ? campaignRepository.findAllByActiveTrue()
                : campaignRepository.findAllBySalonIdInAndActiveTrue(salonIds);
        return campaigns.stream()
                .map(CampaignResponseDto::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponseDto getById(Long id) {
        Campaign campaign = findActiveById(id);
        salonAccessService.requireSalonAccess(campaign.getSalon() != null ? campaign.getSalon().getId() : null);
        return new CampaignResponseDto(campaign);
    }

    @Override
    @Transactional
    public CampaignResponseDto create(CampaignRequestDto request) {
        String code = generateUniqueCode(request.getCode());
        Salon salon = findActiveSalon(salonAccessService.resolveSalonId(request.getSalonId()));

        Campaign campaign = new Campaign();
        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setCode(code);
        campaign.setDiscountType(request.getDiscountType());
        campaign.setDiscountValue(request.getDiscountValue());
        campaign.setMaxUsageCount(request.getMaxUsageCount());
        campaign.setIsCustomerSpecific(request.getIsCustomerSpecific() != null ? request.getIsCustomerSpecific() : false);
        campaign.setValidFrom(request.getValidFrom());
        campaign.setValidTo(request.getValidTo());
        campaign.setSalon(salon);

        if (Boolean.TRUE.equals(request.getIsCustomerSpecific()) && request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Müşteri", request.getCustomerId()));
            requireCustomerSalon(customer, salon.getId());
            campaign.setCustomer(customer);
        }

        return new CampaignResponseDto(campaignRepository.save(campaign));
    }

    @Override
    @Transactional
    public CampaignResponseDto update(Long id, CampaignRequestDto request) {
        Campaign campaign = findActiveById(id);
        salonAccessService.requireSalonAccess(campaign.getSalon() != null ? campaign.getSalon().getId() : null);
        Salon salon = request.getSalonId() == null
                ? campaign.getSalon()
                : findActiveSalon(salonAccessService.resolveSalonId(request.getSalonId()));

        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(campaign.getCode())) {
            if (campaignRepository.existsByCode(request.getCode())) {
                throw new DuplicateResourceException("Bu kampanya kodu zaten kullanılıyor: " + request.getCode());
            }
            campaign.setCode(request.getCode());
        }

        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setDiscountType(request.getDiscountType());
        campaign.setDiscountValue(request.getDiscountValue());
        campaign.setMaxUsageCount(request.getMaxUsageCount());
        campaign.setIsCustomerSpecific(request.getIsCustomerSpecific() != null ? request.getIsCustomerSpecific() : false);
        campaign.setValidFrom(request.getValidFrom());
        campaign.setValidTo(request.getValidTo());
        campaign.setSalon(salon);

        if (Boolean.TRUE.equals(request.getIsCustomerSpecific()) && request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Müşteri", request.getCustomerId()));
            requireCustomerSalon(customer, salon.getId());
            campaign.setCustomer(customer);
        } else {
            campaign.setCustomer(null);
        }

        return new CampaignResponseDto(campaignRepository.save(campaign));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Campaign campaign = findActiveById(id);
        salonAccessService.requireSalonAccess(campaign.getSalon() != null ? campaign.getSalon().getId() : null);
        campaign.setActive(false);
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponseDto validate(String code) {
        Campaign campaign = campaignRepository.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Geçersiz kampanya kodu: " + code));
        salonAccessService.requireSalonAccess(campaign.getSalon() != null ? campaign.getSalon().getId() : null);

        if (Boolean.FALSE.equals(campaign.getActive())) {
            throw new BadRequestException("Bu kampanya artık aktif değil.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (campaign.getValidFrom() != null && now.isBefore(campaign.getValidFrom())) {
            throw new BadRequestException("Kampanya henüz başlamamış. Başlangıç: " + campaign.getValidFrom());
        }
        if (campaign.getValidTo() != null && now.isAfter(campaign.getValidTo())) {
            throw new BadRequestException("Kampanyanın geçerlilik süresi dolmuş. Bitiş: " + campaign.getValidTo());
        }
        if (campaign.getMaxUsageCount() != null && campaign.getUsedCount() >= campaign.getMaxUsageCount()) {
            throw new BadRequestException("Kampanya kullanım limiti dolmuş.");
        }

        return new CampaignResponseDto(campaign);
    }

    private Campaign findActiveById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kampanya", id));
        if (Boolean.FALSE.equals(campaign.getActive())) {
            throw new ResourceNotFoundException("Kampanya", id);
        }
        return campaign;
    }

    private String generateUniqueCode(String requestedCode) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            if (campaignRepository.existsByCode(requestedCode)) {
                throw new DuplicateResourceException("Bu kampanya kodu zaten kullanılıyor: " + requestedCode);
            }
            return requestedCode.toUpperCase();
        }
        String code;
        do {
            code = "IH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (campaignRepository.existsByCode(code));
        return code;
    }

    private Salon findActiveSalon(Long id) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", id));
        if (Boolean.FALSE.equals(salon.getActive())) {
            throw new ResourceNotFoundException("Salon", id);
        }
        return salon;
    }

    private void requireCustomerSalon(Customer customer, Long salonId) {
        if (customer.getSalon() == null || !salonId.equals(customer.getSalon().getId())) {
            throw new BadRequestException("Müşteri ile kampanya aynı salona ait olmalıdır.");
        }
    }
}
