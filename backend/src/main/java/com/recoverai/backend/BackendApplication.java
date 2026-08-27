package com.recoverai.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		String[] potentialDirs = {".", "backend", ".."};
		for (String dir : potentialDirs) {
			File envFile = new File(dir, ".env");
			if (envFile.exists() && envFile.isFile()) {
				Dotenv dotenv = Dotenv.configure()
						.directory(dir)
						.ignoreIfMalformed()
						.ignoreIfMissing()
						.load();
				dotenv.entries().forEach(entry -> {
					if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
						System.setProperty(entry.getKey(), entry.getValue());
					}
				});
				break;
			}
		}
		SpringApplication.run(BackendApplication.class, args);
	}

}
