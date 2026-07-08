package com.ddms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class DdmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DdmsApplication.class, args);
    }

    @Bean
    CommandLineRunner init() {
        return args -> {
            try {
                // Ensure storage and data directories exist
                Files.createDirectories(Paths.get("./document-storage"));
                Files.createDirectories(Paths.get("./data"));
                System.out.println("Storage directories initialized successfully.");
            } catch (IOException e) {
                System.err.println("Could not initialize storage directory: " + e.getMessage());
            }
        };
    }
}
