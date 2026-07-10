package com.ddms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.ddms.model.Category;
import com.ddms.repository.CategoryRepository;

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
        System.out.println("   Access the deployed cloud: https://mini-project-n3hr.onrender.com");
        System.out.println("========================================================================\n");
        openBrowser();
    }

    private static void openBrowser() {
        // Skip automatically opening browser in headless cloud environments (e.g. Render)
        if (System.getenv("PORT") != null || java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }
        String url = "http://localhost:8080";
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec("cmd /c start " + url);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (Exception e) {
            System.out.println("Could not open browser automatically: " + e.getMessage());
        }
    }

    @Bean
    CommandLineRunner init(CategoryRepository categoryRepository) {
        return args -> {
            try {
                initializeStorage();
                
                // Seed default categories if none exist
                if (categoryRepository.count() == 0) {
                    categoryRepository.save(new Category("General"));
                    categoryRepository.save(new Category("Reports"));
                    categoryRepository.save(new Category("Invoices"));
                    categoryRepository.save(new Category("Personal"));
                    System.out.println("Default categories seeded successfully.");
                }
            } catch (Exception e) {
                System.err.println("Could not initialize storage directory: " + e.getMessage());
            }
        };
    }
}
