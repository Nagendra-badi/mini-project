package com.ddms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class DdmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DdmsApplication.class, args);
    }

    private static void initializeStorage() {
        new File("./document-storage").mkdirs();
        new File("./data").mkdirs();
        System.out.println("\n========================================================================");
        System.out.println("   DDMS CLOUD APPLICATION STARTED SUCCESSFULLY!");
        System.out.println("   Access the system locally: http://localhost:8080");
        System.out.println("========================================================================\n");
    }

    @Bean
    CommandLineRunner init() {
        return args -> {
            try {
                // Ensure storage and data directories exist
                initializeStorage();
            } catch (Exception e) {
                System.err.println("Could not initialize storage directory: " + e.getMessage());
            }
        };
    }
}
