package com.bigbear.ihair.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
public class ListApiPerformanceMigration implements ApplicationRunner {
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) return;
        }
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("db/optimize_list_apis.sql"));
        populator.setSeparator(";;");
        populator.execute(dataSource);
    }
}
