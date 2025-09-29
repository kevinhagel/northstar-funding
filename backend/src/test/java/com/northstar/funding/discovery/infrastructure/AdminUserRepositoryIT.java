package com.northstar.funding.discovery.infrastructure;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.northstar.funding.discovery.domain.AdminRole;
import com.northstar.funding.discovery.domain.AdminUser;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class AdminUserRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("northstar_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private AdminUserRepository repository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM admin_user");
    }

    @Test
    void shouldSaveAndRetrieveAdminUser() {
        // Given
        AdminUser adminUser = AdminUser.builder()
            .username("test.admin")
            .fullName("Test Admin")
            .email("test@example.com")
            .role(AdminRole.REVIEWER)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            // Required NOT NULL fields from database
            .candidatesReviewed(0)
            .averageReviewTimeMinutes(0)
            .approvalRate(0.0)
            .currentWorkload(0)
            .maxConcurrentAssignments(10)
            // Specializations as Set<String> -> PostgreSQL TEXT[]
            .specializations(Set.of("technology", "research"))
            .build();

        // When
        AdminUser saved = repository.save(adminUser);

        // Then
        assertThat(saved.getUserId()).isNotNull();
        
        Optional<AdminUser> retrieved = repository.findById(saved.getUserId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getUsername()).isEqualTo("test.admin");
        assertThat(retrieved.get().getRole()).isEqualTo(AdminRole.REVIEWER);
        assertThat(retrieved.get().getSpecializations()).containsExactlyInAnyOrder("technology", "research");
    }
}
