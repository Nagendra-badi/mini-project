package com.ddms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${db.host}")
    private String dbHost;

    @Value("${db.port}")
    private String dbPort;

    @Value("${db.name}")
    private String dbName;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        // If RENDER environment variable is present, we are running on the cloud (Render)
        if (System.getenv("RENDER") != null) {
            System.out.println("RENDER cloud environment detected. Connecting to PostgreSQL database...");
            String pgUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
            return DataSourceBuilder.create()
                    .url(pgUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }
        
        // Otherwise, we are running locally on localhost. Connect to offline H2 Database.
        System.out.println("Local environment detected. Connecting to Offline In-Memory H2 Database...");
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:ddms;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                .username("sa")
                .password("")
                .driverClassName("org.h2.Driver")
                .build();
    }
}
