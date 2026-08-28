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
    @DisplayName("Flyway V1, V2, V3, V4, V5, and V6 migration scripts should execute successfully")
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
            assertEquals(6, migrationsApplied, "Expected 6 migrations (V1, V2, V3, V4, V5, V6) to be executed successfully");
        });
    }
}
