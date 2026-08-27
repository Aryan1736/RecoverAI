package com.recoverai.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DotenvEnvironmentPostProcessorTest {

    @Test
    void postProcessEnvironment_runsSuccessfullyWithoutCrashing() {
        DotenvEnvironmentPostProcessor processor = new DotenvEnvironmentPostProcessor();
        ConfigurableEnvironment environment = new StandardEnvironment();
        SpringApplication application = new SpringApplication();

        assertDoesNotThrow(() -> processor.postProcessEnvironment(environment, application));
        assertNotNull(environment.getPropertySources());
    }
}
