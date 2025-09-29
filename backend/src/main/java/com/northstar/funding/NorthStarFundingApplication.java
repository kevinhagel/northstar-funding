package com.northstar.funding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * NorthStar Funding Discovery Application
 * 
 * Automated funding discovery workflow with human-AI collaboration.
 * 
 * Constitutional Principles Applied:
 * - Java 25 + Spring Boot 3.5.5 (Technology Stack)
 * - Domain-Driven Design with "Funding Sources" ubiquitous language
 * - Human-AI Collaboration workflows
 * - Contact Intelligence as first-class entities
 * - Complexity Management (single monolith service)
 * 
 * @author Kevin & Huw - NorthStar Foundation
 * @version 1.0.0
 * @since Java 25
 */
@SpringBootApplication
@EnableWebSecurity  
@EnableTransactionManagement
@ConfigurationPropertiesScan
public class NorthStarFundingApplication {

    /**
     * Main application entry point
     * 
     * Uses Java 25 Virtual Threads for I/O operations (Constitutional requirement)
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Constitutional requirement: Virtual Threads for I/O operations
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(NorthStarFundingApplication.class, args);
    }
}
