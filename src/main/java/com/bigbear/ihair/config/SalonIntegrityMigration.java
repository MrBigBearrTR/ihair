package com.bigbear.ihair.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 6)
@RequiredArgsConstructor
public class SalonIntegrityMigration implements ApplicationRunner {
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) return;
        }
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("db/enforce_salon_integrity.sql"));
        populator.setSeparator(";;");
        populator.execute(dataSource);
        log.info("Salon veri bütünlüğü migration işlemi tamamlandı.");
    }
}
