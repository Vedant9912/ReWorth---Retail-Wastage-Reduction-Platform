package com.nearsave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * NearSaveApplication — Spring Boot entry point.
 *
 * @SpringBootApplication combines:
 *   @Configuration  → marks this class as a config source
 *   @EnableAutoConfiguration → wires up JPA, Security, Web automatically
 *   @ComponentScan  → discovers all @Component, @Service, @Repository, @Controller
 *
 * @EnableScheduling → activates the @Scheduled token-expiry background job
 */
@SpringBootApplication
@EnableScheduling
public class NearSaveApplication {

    public static void main(String[] args) {
        SpringApplication.run(NearSaveApplication.class, args);
        System.out.println("✅ NearSave server started → http://localhost:8080");
    }
}
