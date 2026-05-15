package com.reworth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReworthApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReworthApplication.class, args);
    }
}
