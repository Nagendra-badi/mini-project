package com.ddms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.ddms.model.Category;
import com.ddms.model.User;
import com.ddms.repository.CategoryRepository;
import com.ddms.repository.UserRepository;

@SpringBootApplication
public class DdmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DdmsApplication.class, args);
    }

    private static void initializeStorage() {
        new File("./document-storage").mkdirs();
        new File("./data").mkdirs();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
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
    CommandLineRunner init(CategoryRepository categoryRepository, UserRepository userRepository) {
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
                
                // 1. Demote any other existing administrators in the database to USER
                java.util.List<User> allUsers = userRepository.findAll();
                for (User u : allUsers) {
                    if ("ADMIN".equalsIgnoreCase(u.getRole())) {
                        if (!"BhaskaraRao".equalsIgnoreCase(u.getUsername()) && !"Nagendra".equalsIgnoreCase(u.getUsername())) {
                            u.setRole("USER");
                            userRepository.save(u);
                            System.out.println("Demoted non-default admin user: " + u.getUsername());
                        }
                    }
                }

                // 2. Seed Admin 1: BhaskaraRao (Password: Bhaskar1602@)
                java.util.Optional<User> admin1 = userRepository.findByUsername("BhaskaraRao");
                if (admin1.isPresent()) {
                    User u1 = admin1.get();
                    u1.setRole("ADMIN");
                    u1.setPassword("Bhaskar1602@");
                    userRepository.save(u1);
                } else {
                    User u1 = new User();
                    u1.setUsername("BhaskaraRao");
                    u1.setPassword("Bhaskar1602@");
                    u1.setEmail("bhaskararao@ddms.com");
                    u1.setFullName("Bhaskara Rao");
                    u1.setRole("ADMIN");
                    userRepository.save(u1);
                    System.out.println("Seeded admin user: BhaskaraRao");
                }

                // 3. Seed Admin 2: Nagendra (Password: nagendra)
                java.util.Optional<User> admin2 = userRepository.findByUsername("Nagendra");
                if (admin2.isPresent()) {
                    User u2 = admin2.get();
                    u2.setRole("ADMIN");
                    u2.setPassword("nagendra");
                    userRepository.save(u2);
                } else {
                    User u2 = new User();
                    u2.setUsername("Nagendra");
                    u2.setPassword("nagendra");
                    u2.setEmail("nagendra@ddms.com");
                    u2.setFullName("Nagendra");
                    u2.setRole("ADMIN");
                    userRepository.save(u2);
                    System.out.println("Seeded admin user: Nagendra");
                }
            } catch (Exception e) {
                System.err.println("Could not initialize storage directory: " + e.getMessage());
            }
        };
    }
}
