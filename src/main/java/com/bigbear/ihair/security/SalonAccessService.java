package com.bigbear.ihair.security;

import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.SalonScopeException;
import com.bigbear.ihair.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SalonAccessService {

    private final UserRepository userRepository;

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Kimlik doğrulaması gereklidir.");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Oturum kullanıcısı bulunamadı."));
    }

    public boolean isAdmin() {
        return currentUser().getRole() == Role.ADMIN;
    }

    public Role currentRole() {
        return currentUser().getRole();
    }

    public Long currentEmployeeId() {
        User user = currentUser();
        if (user.getRole() != Role.EMPLOYEE || user.getEmployee() == null) {
            throw new AccessDeniedException("Bu işlem çalışan hesabı gerektirir.");
        }
        return user.getEmployee().getId();
    }

    public Long currentSalonId() {
        User user = currentUser();
        if (user.getRole() == Role.EMPLOYEE) {
            return employeeSalonId(user);
        }
        return resolveSalonId(null);
    }

    public Long resolveSalonId(Long requestedSalonId) {
        User user = currentUser();
        if (user.getRole() == Role.ADMIN) {
            if (requestedSalonId == null) {
                throw new BadRequestException("salonId alanı zorunludur.");
            }
            return requestedSalonId;
        }
        if (user.getRole() == Role.EMPLOYEE) {
            Long salonId = employeeSalonId(user);
            requireRequestedAccess(requestedSalonId, salonId);
            return salonId;
        }
        if (user.getRole() != Role.SALON_OWNER) {
            throw new AccessDeniedException("Bu işlem için salon yetkisi gereklidir.");
        }

        Set<Long> salonIds = ownerSalonIds(user);
        if (requestedSalonId != null) {
            if (!salonIds.contains(requestedSalonId)) {
                throw new AccessDeniedException("Başka bir salona ait kayıt üzerinde işlem yapılamaz.");
            }
            return requestedSalonId;
        }
        if (salonIds.isEmpty()) {
            throw new SalonScopeException(
                    "SALON_ACCESS_MISSING",
                    "Kullanıcının yetkili olduğu bir salon bulunmuyor.");
        }
        if (salonIds.size() > 1) {
            throw new SalonScopeException(
                    "SALON_SCOPE_AMBIGUOUS",
                    "Birden fazla salon yetkiniz var; salonId belirtmelisiniz.");
        }
        return salonIds.iterator().next();
    }

    public Set<Long> resolveSalonIdsForList(Long requestedSalonId) {
        User user = currentUser();
        if (user.getRole() == Role.ADMIN) {
            return requestedSalonId == null ? null : Set.of(requestedSalonId);
        }
        if (user.getRole() == Role.EMPLOYEE) {
            Long salonId = employeeSalonId(user);
            requireRequestedAccess(requestedSalonId, salonId);
            return Set.of(salonId);
        }
        if (user.getRole() == Role.SALON_OWNER) {
            Set<Long> salonIds = ownerSalonIds(user);
            if (salonIds.isEmpty()) {
                throw new SalonScopeException(
                        "SALON_ACCESS_MISSING",
                        "Kullanıcının yetkili olduğu bir salon bulunmuyor.");
            }
            if (requestedSalonId != null) {
                if (!salonIds.contains(requestedSalonId)) {
                    throw new AccessDeniedException("Başka bir salona ait kayıt üzerinde işlem yapılamaz.");
                }
                return Set.of(requestedSalonId);
            }
            return salonIds;
        }
        throw new AccessDeniedException("Bu işlem için salon yetkisi gereklidir.");
    }

    public void requireSalonAccess(Long salonId) {
        User user = currentUser();
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        Set<Long> allowed = user.getRole() == Role.EMPLOYEE
                ? Set.of(employeeSalonId(user))
                : ownerSalonIds(user);
        if (salonId == null || !allowed.contains(salonId)) {
            throw new AccessDeniedException("Başka bir salona ait kayıt üzerinde işlem yapılamaz.");
        }
    }

    private Set<Long> ownerSalonIds(User user) {
        Set<Long> ids = new LinkedHashSet<>();
        user.getAuthorizedSalons().forEach(salon -> ids.add(salon.getId()));
        if (user.getSalon() != null) {
            ids.add(user.getSalon().getId());
        }
        return ids;
    }

    private Long employeeSalonId(User user) {
        if (user.getEmployee() == null || user.getEmployee().getSalon() == null) {
            throw new SalonScopeException(
                    "SALON_ACCESS_MISSING",
                    "Çalışan kullanıcının salon ataması bulunmuyor.");
        }
        return user.getEmployee().getSalon().getId();
    }

    private void requireRequestedAccess(Long requestedSalonId, Long actualSalonId) {
        if (requestedSalonId != null && !actualSalonId.equals(requestedSalonId)) {
            throw new AccessDeniedException("Başka bir salona ait kayıt üzerinde işlem yapılamaz.");
        }
    }
}
