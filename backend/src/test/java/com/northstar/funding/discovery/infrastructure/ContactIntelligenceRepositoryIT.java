package com.northstar.funding.discovery.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.northstar.funding.discovery.config.TestDataFactory;
import com.northstar.funding.discovery.domain.AuthorityLevel;
import com.northstar.funding.discovery.domain.ContactIntelligence;
import com.northstar.funding.discovery.domain.ContactType;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class ContactIntelligenceRepositoryIT {

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
    private ContactIntelligenceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestDataFactory testDataFactory;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM contact_intelligence");
        jdbcTemplate.execute("DELETE FROM funding_source_candidate");
    }

    @Test
    void shouldSaveAndRetrieveContactIntelligence() {
        // Given - Create a funding source candidate first (FK requirement)
        UUID candidateId = UUID.randomUUID();
        jdbcTemplate.execute(String.format(
            "INSERT INTO funding_source_candidate (candidate_id, status, confidence_score, organization_name, program_name, source_url, discovery_method, search_query, discovered_at, discovery_session_id) " +
            "VALUES ('%s', 'PENDING_REVIEW', 0.8, 'Test Org', 'Test Program', 'http://test.com', 'test', 'test query', NOW(), NULL)", 
            candidateId
        ));

        ContactIntelligence contact = testDataFactory.contactIntelligenceBuilder()
            .candidateId(candidateId)
            .fullName("Dr. Jane Smith")
            .contactType(ContactType.PROGRAM_OFFICER)
            .decisionAuthority(AuthorityLevel.DECISION_MAKER)
            .organization("National Science Foundation")
            .email("jane.smith@nsf.gov")
            .phone("+1-555-0123")
            .isActive(true)
            .validatedAt(LocalDateTime.now().minusDays(30))
            .build();

        // When
        ContactIntelligence saved = repository.save(contact);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getContactId()).isNotNull();
        assertThat(saved.getFullName()).isEqualTo("Dr. Jane Smith");
        assertThat(saved.getContactType()).isEqualTo(ContactType.PROGRAM_OFFICER);
        assertThat(saved.getDecisionAuthority()).isEqualTo(AuthorityLevel.DECISION_MAKER);
        assertThat(saved.getIsActive()).isTrue();
    }
}
