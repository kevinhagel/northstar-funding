package com.northstar.funding.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application for NorthStar Admin Dashboard REST API.
 *
 * Feature 013: Admin Dashboard Review Queue
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.northstar.funding.rest",
    "com.northstar.funding.persistence"
})
public class NorthstarRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NorthstarRestApiApplication.class, args);
    }
}
