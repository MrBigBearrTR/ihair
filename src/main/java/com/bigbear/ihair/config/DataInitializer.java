package com.bigbear.ihair.config;

import com.bigbear.ihair.entity.User;
import com.bigbear.ihair.entity.enums.Role;
import com.bigbear.ihair.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        String resetPassword = System.getenv("IHAIR_ADMIN_RESET_PASSWORD");

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setPassword(passwordEncoder.encode(
                    StringUtils.hasText(resetPassword) ? resetPassword : "admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info(">>> Varsayılan admin kullanıcısı oluşturuldu.");
            log.info(">>> Kullanıcı adı: admin");
            log.info(">>> Güvenlik için ilk girişten sonra şifrenizi değiştirin!");
            return;
        }

        if (StringUtils.hasText(resetPassword)) {
            User admin = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new IllegalStateException(
                            "ADMIN rolü mevcut ancak 'admin' kullanıcısı bulunamadı."));
            admin.setPassword(passwordEncoder.encode(resetPassword));
            userRepository.save(admin);
            log.warn(">>> Admin şifresi IHAIR_ADMIN_RESET_PASSWORD ile sıfırlandı.");
        }
    }
}
