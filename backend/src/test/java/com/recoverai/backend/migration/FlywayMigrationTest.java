package com.recoverai.backend.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FlywayMigrationTest {

    @Test
    @DisplayName("Flyway V1 migration script should execute successfully")
    void testFlywayMigrationExecution() {
        DataSource dataSource = DataSourceBuilder.create()
                .url("jdbc:h2:mem:flyway_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        assertDoesNotThrow(() -> {
            int migrationsApplied = flyway.migrate().migrationsExecuted;
            assertEquals(1, migrationsApplied, "Expected 1 migration (V1) to be executed successfully");
        });
    }
}
