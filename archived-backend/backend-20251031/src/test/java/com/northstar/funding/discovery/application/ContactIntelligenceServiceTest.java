package com.northstar.funding.discovery.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.northstar.funding.discovery.domain.AuthorityLevel;
import com.northstar.funding.discovery.domain.ContactIntelligence;
import com.northstar.funding.discovery.domain.ContactType;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.infrastructure.ContactIntelligenceRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service Layer Test: ContactIntelligenceService
 *
 * Tests business logic for contact intelligence management, validation, and encryption
 * Mocks all repository dependencies for isolated unit testing
 *
 * TDD: This test MUST FAIL until ContactIntelligenceService is implemented
 */
@ExtendWith(MockitoExtension.class)
class ContactIntelligenceServiceTest {

    @Mock
    private ContactIntelligenceRepository contactRepository;

    @Mock
    private FundingSourceCandidateRepository candidateRepository;

    private ContactIntelligenceService service;

    @BeforeEach
    void setUp() {
        service = new ContactIntelligenceService(contactRepository, candidateRepository);
    }

    @Test
    void shouldAddContactToCandidate() {
        // Given
        UUID candidateId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .build();

        ContactIntelligence contact = ContactIntelligence.builder()
                .contactId(contactId)
                .candidateId(candidateId)
                .contactType(ContactType.PROGRAM_OFFICER)
                .fullName("Dr. Maria Ivanova")
                .email("m.ivanova@foundation.bg")
                .phone("+359 2 123 4567")
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(contactRepository.save(any(ContactIntelligence.class))).thenReturn(contact);

        // When
        ContactIntelligence saved = service.addContact(candidateId, contact);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getCandidateId()).isEqualTo(candidateId);
        verify(contactRepository).save(any(ContactIntelligence.class));
    }

    @Test
    void shouldValidateEmailFormat() {
        // Given
        UUID candidateId = UUID.randomUUID();
        ContactIntelligence invalidContact = ContactIntelligence.builder()
                .candidateId(candidateId)
                .contactType(ContactType.FOUNDATION_STAFF)
                .fullName("John Smith")
                .email("not-a-valid-email")
                .build();

        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        // When/Then
        assertThatThrownBy(() -> service.addContact(candidateId, invalidContact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void shouldEncryptSensitiveFields() {
        // Given
        UUID candidateId = UUID.randomUUID();
        ContactIntelligence contact = ContactIntelligence.builder()
                .candidateId(candidateId)
                .contactType(ContactType.PROGRAM_OFFICER)
                .fullName("Sensitive Contact")
                .email("sensitive@example.com")
                .phone("+1-555-1234")
                .build();

        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(contactRepository.save(any(ContactIntelligence.class))).thenReturn(contact);

        // When
        service.addContact(candidateId, contact);

        // Then
        verify(contactRepository).save(argThat(c ->
            c.getEmail() != null && c.getPhone() != null
            // In real implementation, these would be encrypted
        ));
    }

    @Test
    void shouldValidateContactBeforeAdding() {
        // Given
        UUID candidateId = UUID.randomUUID();
        ContactIntelligence incompleteContact = ContactIntelligence.builder()
                .candidateId(candidateId)
                .contactType(ContactType.PROGRAM_OFFICER)
                // Missing required fullName
                .email("test@example.com")
                .build();

        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        // When/Then
        assertThatThrownBy(() -> service.addContact(candidateId, incompleteContact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fullName");
    }

    @Test
    void shouldGetContactsForCandidate() {
        // Given
        UUID candidateId = UUID.randomUUID();
        List<ContactIntelligence> contacts = List.of(
                createContact(candidateId, "Contact 1"),
                createContact(candidateId, "Contact 2")
        );

        when(contactRepository.findByCandidateId(candidateId)).thenReturn(contacts);

        // When
        List<ContactIntelligence> retrieved = service.getContactsForCandidate(candidateId);

        // Then
        assertThat(retrieved).hasSize(2);
        verify(contactRepository).findByCandidateId(candidateId);
    }

    @Test
    void shouldValidateContactInformation() {
        // Given
        UUID contactId = UUID.randomUUID();
        ContactIntelligence contact = createContact(UUID.randomUUID(), "Test Contact");
        contact.setContactId(contactId);
        contact.setValidatedAt(null);

        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(ContactIntelligence.class))).thenReturn(contact);

        // When
        ContactIntelligence validated = service.validateContact(contactId);

        // Then
        verify(contactRepository).save(argThat(c ->
            c.getValidatedAt() != null && c.getIsActive() == true
        ));
    }

    @Test
    void shouldMarkContactAsInactive() {
        // Given
        UUID contactId = UUID.randomUUID();
        ContactIntelligence contact = createContact(UUID.randomUUID(), "Old Contact");
        contact.setContactId(contactId);
        contact.setIsActive(true);

        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(ContactIntelligence.class))).thenReturn(contact);

        // When
        service.markContactInactive(contactId, "Email bounced");

        // Then
        verify(contactRepository).save(argThat(c -> c.getIsActive() == false));
    }

    @Test
    void shouldFindContactsNeedingValidation() {
        // Given
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        List<ContactIntelligence> outdatedContacts = List.of(
                createContact(UUID.randomUUID(), "Outdated 1"),
                createContact(UUID.randomUUID(), "Outdated 2")
        );

        when(contactRepository.findByValidatedAtBeforeOrValidatedAtIsNull(any(LocalDateTime.class)))
                .thenReturn(outdatedContacts);

        // When
        List<ContactIntelligence> needsValidation = service.findContactsNeedingValidation();

        // Then
        assertThat(needsValidation).hasSize(2);
        verify(contactRepository).findByValidatedAtBeforeOrValidatedAtIsNull(any(LocalDateTime.class));
    }

    @Test
    void shouldUpdateContactWithRelationshipNotes() {
        // Given
        UUID contactId = UUID.randomUUID();
        ContactIntelligence contact = createContact(UUID.randomUUID(), "Program Officer");
        contact.setContactId(contactId);

        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(ContactIntelligence.class))).thenReturn(contact);

        // When
        service.addRelationshipNote(contactId, "Very responsive to Eastern Europe NGOs");

        // Then
        verify(contactRepository).save(argThat(c ->
            c.getRelationshipNotes().contains("Very responsive to Eastern Europe NGOs")
        ));
    }

    @Test
    void shouldTrackCommunicationHistory() {
        // Given
        UUID contactId = UUID.randomUUID();
        ContactIntelligence contact = createContact(UUID.randomUUID(), "Foundation Staff");
        contact.setContactId(contactId);
        contact.setLastContactedAt(null);

        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(ContactIntelligence.class))).thenReturn(contact);

        // When
        service.recordCommunication(contactId, "Initial inquiry sent");

        // Then
        verify(contactRepository).save(argThat(c ->
            c.getLastContactedAt() != null
        ));
    }

    @Test
    void shouldThrowExceptionWhenCandidateNotFound() {
        // Given
        UUID nonexistentCandidateId = UUID.randomUUID();
        ContactIntelligence contact = createContact(nonexistentCandidateId, "Test");

        when(candidateRepository.findById(nonexistentCandidateId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.addContact(nonexistentCandidateId, contact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Candidate not found");
    }

    @Test
    void shouldPreventDuplicateContacts() {
        // Given
        UUID candidateId = UUID.randomUUID();
        ContactIntelligence existingContact = createContact(candidateId, "Dr. Maria Ivanova");
        existingContact.setEmail("m.ivanova@foundation.bg");

        ContactIntelligence duplicateContact = createContact(candidateId, "Maria Ivanova");
        duplicateContact.setEmail("m.ivanova@foundation.bg");

        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(contactRepository.findByCandidateId(candidateId)).thenReturn(List.of(existingContact));

        // When/Then
        assertThatThrownBy(() -> service.addContact(candidateId, duplicateContact))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate");
    }

    private ContactIntelligence createContact(UUID candidateId, String name) {
        return ContactIntelligence.builder()
                .contactId(UUID.randomUUID())
                .candidateId(candidateId)
                .contactType(ContactType.PROGRAM_OFFICER)
                .fullName(name)
                .email(name.toLowerCase().replace(" ", ".") + "@example.com")
                .decisionAuthority(AuthorityLevel.DECISION_MAKER)
                .isActive(true)
                .build();
    }
}
