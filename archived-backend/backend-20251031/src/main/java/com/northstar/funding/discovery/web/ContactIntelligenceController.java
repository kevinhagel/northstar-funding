package com.northstar.funding.discovery.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.northstar.funding.discovery.application.ContactIntelligenceService;
import com.northstar.funding.discovery.domain.AuthorityLevel;
import com.northstar.funding.discovery.domain.ContactIntelligence;
import com.northstar.funding.discovery.domain.ContactType;

/**
 * REST Controller: ContactIntelligenceController
 *
 * Handles contact intelligence for funding source candidates
 * Endpoints: GET/POST /api/candidates/{id}/contacts
 */
@RestController
@RequestMapping("/api/candidates/{candidateId}/contacts")
public class ContactIntelligenceController {

    private final ContactIntelligenceService contactService;

    public ContactIntelligenceController(ContactIntelligenceService contactService) {
        this.contactService = contactService;
    }

    /**
     * GET /api/candidates/{candidateId}/contacts - Get all contacts for candidate
     */
    @GetMapping
    public ResponseEntity<List<ContactIntelligence>> getCandidateContacts(
            @PathVariable String candidateId) {

        try {
            UUID uuid = UUID.fromString(candidateId);
            List<ContactIntelligence> contacts = contactService.getContactsForCandidate(uuid);
            return ResponseEntity.ok(contacts);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/candidates/{candidateId}/contacts - Add new contact
     */
    @PostMapping
    public ResponseEntity<?> addCandidateContact(
            @PathVariable String candidateId,
            @RequestBody ContactIntelligenceRequest request) {

        // Validate required fields
        if (request.contactType() == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("contactType is required"));
        }
        if (request.fullName() == null || request.fullName().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("fullName is required"));
        }

        // Validate email format if provided
        if (request.email() != null && !request.email().isBlank()) {
            if (!request.email().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid email format"));
            }
        }

        // Validate max lengths
        if (request.fullName() != null && request.fullName().length() > 255) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("fullName too long (max 255 chars)"));
        }

        try {
            UUID candidateUuid = UUID.fromString(candidateId);

            // Build contact from request
            ContactIntelligence contact = ContactIntelligence.builder()
                    .contactType(request.contactType())
                    .fullName(request.fullName())
                    .title(request.title())
                    .email(request.email())
                    .phone(request.phone())
                    .organization(request.organization())
                    .officeAddress(request.officeAddress())
                    .communicationPreference(request.communicationPreference())
                    .responsePattern(request.responsePattern())
                    .referralSource(request.referralSource())
                    .decisionAuthority(request.decisionAuthority())
                    .relationshipNotes(request.relationshipNotes())
                    .referralConnections(request.referralConnections())
                    .build();

            ContactIntelligence saved = contactService.addContact(candidateUuid, contact);

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("email")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            // Duplicate contact
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * DTOs
     */
    public record ContactIntelligenceRequest(
        ContactType contactType,
        String fullName,
        String title,
        String email,
        String phone,
        String organization,
        String officeAddress,
        String communicationPreference,
        String responsePattern,
        String referralSource,
        AuthorityLevel decisionAuthority,
        String relationshipNotes,
        String referralConnections
    ) {}

    public record ErrorResponse(String error) {}
}
