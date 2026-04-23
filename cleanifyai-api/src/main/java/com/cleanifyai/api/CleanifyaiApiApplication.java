package com.cleanifyai.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CleanifyaiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleanifyaiApiApplication.class, args);
    }
}

