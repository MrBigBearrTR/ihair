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

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info(">>> Varsayılan admin kullanıcısı oluşturuldu.");
            log.info(">>> Kullanıcı adı: admin | Şifre: admin123");
            log.info(">>> Güvenlik için ilk girişten sonra şifrenizi değiştirin!");
        }
    }
}
