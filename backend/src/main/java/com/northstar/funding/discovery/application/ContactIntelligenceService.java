package com.northstar.funding.discovery.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.discovery.domain.ContactIntelligence;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.infrastructure.ContactIntelligenceRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

/**
 * Service Layer: ContactIntelligenceService
 *
 * Handles business logic for contact intelligence management, validation, and lifecycle
 * Enforces data quality rules and manages contact relationships
 */
@Service
@Transactional
public class ContactIntelligenceService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final int VALIDATION_THRESHOLD_DAYS = 90;

    private final ContactIntelligenceRepository contactRepository;
    private final FundingSourceCandidateRepository candidateRepository;

    public ContactIntelligenceService(
            ContactIntelligenceRepository contactRepository,
            FundingSourceCandidateRepository candidateRepository) {
        this.contactRepository = contactRepository;
        this.candidateRepository = candidateRepository;
    }

    /**
     * Add new contact to a funding source candidate
     * Validates contact information and prevents duplicates
     */
    public ContactIntelligence addContact(UUID candidateId, ContactIntelligence contact) {
        // Verify candidate exists
        FundingSourceCandidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));

        // Validate contact data
        validateContactData(contact);

        // Check for duplicates
        List<ContactIntelligence> existingContacts = contactRepository.findByCandidateId(candidateId);
        if (isDuplicateContact(contact, existingContacts)) {
            throw new IllegalStateException(
                "duplicate contact with email " + contact.getEmail() + " already exists for this candidate"
            );
        }

        // Set candidate ID and defaults
        contact.setCandidateId(candidateId);
        if (contact.getIsActive() == null) {
            contact.setIsActive(true);
        }

        // Note: In production, email and phone would be encrypted here
        // For now, we store them as-is per constitutional requirements

        return contactRepository.save(contact);
    }

    /**
     * Get all contacts for a funding source candidate
     */
    public List<ContactIntelligence> getContactsForCandidate(UUID candidateId) {
        return contactRepository.findByCandidateId(candidateId);
    }

    /**
     * Validate contact information and mark as validated
     */
    public ContactIntelligence validateContact(UUID contactId) {
        ContactIntelligence contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactId));

        contact.setValidatedAt(LocalDateTime.now());
        contact.setIsActive(true);

        return contactRepository.save(contact);
    }

    /**
     * Mark contact as inactive (e.g., email bounced, person left organization)
     */
    public void markContactInactive(UUID contactId, String reason) {
        ContactIntelligence contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactId));

        contact.setIsActive(false);
        contact.setRelationshipNotes(
            (contact.getRelationshipNotes() != null ? contact.getRelationshipNotes() + "\n" : "") +
            "Marked inactive: " + reason + " (at " + LocalDateTime.now() + ")"
        );

        contactRepository.save(contact);
    }

    /**
     * Find contacts that need validation (older than 90 days or never validated)
     */
    public List<ContactIntelligence> findContactsNeedingValidation() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(VALIDATION_THRESHOLD_DAYS);
        return contactRepository.findByValidatedAtBeforeOrValidatedAtIsNull(threshold);
    }

    /**
     * Add relationship notes to contact
     */
    public void addRelationshipNote(UUID contactId, String note) {
        ContactIntelligence contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactId));

        String currentNotes = contact.getRelationshipNotes() != null ? contact.getRelationshipNotes() : "";
        String timestamp = LocalDateTime.now().toString();
        contact.setRelationshipNotes(currentNotes + "\n[" + timestamp + "] " + note);

        contactRepository.save(contact);
    }

    /**
     * Record communication with contact
     */
    public void recordCommunication(UUID contactId, String communicationNote) {
        ContactIntelligence contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactId));

        contact.setLastContactedAt(LocalDateTime.now());
        addRelationshipNote(contactId, "Communication: " + communicationNote);
    }

    /**
     * Validate contact data before saving
     */
    private void validateContactData(ContactIntelligence contact) {
        // Required fields
        if (contact.getFullName() == null || contact.getFullName().isBlank()) {
            throw new IllegalArgumentException("Contact fullName is required");
        }
        if (contact.getContactType() == null) {
            throw new IllegalArgumentException("Contact contactType is required");
        }

        // Email format validation
        if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
            if (!EMAIL_PATTERN.matcher(contact.getEmail()).matches()) {
                throw new IllegalArgumentException("Invalid email format: " + contact.getEmail());
            }
        }
    }

    /**
     * Check if contact is duplicate based on email
     */
    private boolean isDuplicateContact(ContactIntelligence newContact, List<ContactIntelligence> existingContacts) {
        if (newContact.getEmail() == null || newContact.getEmail().isBlank()) {
            return false; // Can't check duplicates without email
        }

        return existingContacts.stream()
                .anyMatch(existing ->
                    existing.getEmail() != null &&
                    existing.getEmail().equalsIgnoreCase(newContact.getEmail())
                );
    }
}
