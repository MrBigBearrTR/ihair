package com.bigbear.ihair.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String SALON_LOGOS = "salonLogos";
    public static final String GLOBAL_LOGO = "globalLogo";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(SALON_LOGOS, GLOBAL_LOGO);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(Duration.ofHours(6)));
        return manager;
    }
}
