package com.recoverai.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor to load configuration from local .env files if present.
 * Looks in current working directory, ./backend, and parent directory.
 * Adds loaded properties to the Spring Environment as lowest precedence so that
 * system environment variables (e.g. Render, CI/CD) and JVM system properties take priority.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] potentialDirs = {".", "backend", ".."};
        Dotenv dotenv = null;

        for (String dir : potentialDirs) {
            File envFile = new File(dir, ".env");
            if (envFile.exists() && envFile.isFile()) {
                dotenv = Dotenv.configure()
                        .directory(dir)
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();
                break;
            }
        }

        if (dotenv == null) {
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();
        }

        Map<String, Object> envMap = new HashMap<>();
        for (DotenvEntry entry : dotenv.entries()) {
            envMap.put(entry.getKey(), entry.getValue());
            // Also populate system property if not already set by OS or JVM
            if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }

        if (!envMap.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("dotenvProperties", envMap));
        }
    }
}
