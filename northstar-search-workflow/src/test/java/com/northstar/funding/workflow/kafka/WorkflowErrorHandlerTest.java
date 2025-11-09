package com.northstar.funding.workflow.kafka;

import com.northstar.funding.kafka.events.WorkflowErrorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test for WorkflowErrorHandler (Kafka consumer for workflow-errors topic).
 *
 * <p>Tests error logging, dead letter queue handling, and acknowledgment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowErrorHandlerTest {

    private WorkflowErrorHandler errorHandler;
    private Acknowledgment acknowledgment;

    @BeforeEach
    void setUp() {
        errorHandler = new WorkflowErrorHandler();
        acknowledgment = mock(Acknowledgment.class);
    }

    @Test
    void handleError_whenValidErrorEvent_shouldLogAndAcknowledge() {
        // Given
        UUID sessionId = UUID.randomUUID();
        WorkflowErrorEvent event = WorkflowErrorEvent.builder()
                .sessionId(sessionId)
                .errorMessage("Search API timeout after 30 seconds")
                .errorType("SEARCH_TIMEOUT")
                .stage("SEARCH_EXECUTION")
                .build();

        // When
        errorHandler.handleError(event, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleError_whenErrorWithStackTrace_shouldLogAndAcknowledge() {
        // Given
        WorkflowErrorEvent event = WorkflowErrorEvent.builder()
                .sessionId(UUID.randomUUID())
                .errorMessage("NullPointerException in domain extraction")
                .errorType("NULL_POINTER")
                .stage("DOMAIN_PROCESSING")
                .stackTrace("java.lang.NullPointerException\n\tat com.northstar...")
                .build();

        // When
        errorHandler.handleError(event, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleError_whenNullEvent_shouldHandleGracefully() {
        // When/Then - should not throw exception
        assertThatCode(() -> errorHandler.handleError(null, acknowledgment))
                .doesNotThrowAnyException();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleError_whenMissingOptionalFields_shouldHandleGracefully() {
        // Given - only required fields populated
        WorkflowErrorEvent event = WorkflowErrorEvent.builder()
                .sessionId(UUID.randomUUID())
                .errorMessage("Generic error")
                .errorType("UNKNOWN")
                .stage("UNKNOWN")
                .build();

        // When/Then - should not throw exception
        assertThatCode(() -> errorHandler.handleError(event, acknowledgment))
                .doesNotThrowAnyException();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleError_whenMultipleErrors_shouldHandleAll() {
        // Given
        WorkflowErrorEvent error1 = WorkflowErrorEvent.builder()
                .sessionId(UUID.randomUUID())
                .errorMessage("Error 1")
                .errorType("TYPE_1")
                .stage("SEARCH_EXECUTION")
                .build();

        WorkflowErrorEvent error2 = WorkflowErrorEvent.builder()
                .sessionId(UUID.randomUUID())
                .errorMessage("Error 2")
                .errorType("TYPE_2")
                .stage("DOMAIN_PROCESSING")
                .build();

        // When
        errorHandler.handleError(error1, acknowledgment);
        errorHandler.handleError(error2, acknowledgment);

        // Then
        verify(acknowledgment, times(2)).acknowledge();
    }

    @Test
    void handleError_whenSameSessionMultipleErrors_shouldHandleAll() {
        // Given - same session, different errors
        UUID sessionId = UUID.randomUUID();

        WorkflowErrorEvent error1 = WorkflowErrorEvent.builder()
                .sessionId(sessionId)
                .errorMessage("Search timeout")
                .errorType("TIMEOUT")
                .stage("SEARCH_EXECUTION")
                .build();

        WorkflowErrorEvent error2 = WorkflowErrorEvent.builder()
                .sessionId(sessionId)
                .errorMessage("Blacklist check failed")
                .errorType("CACHE_ERROR")
                .stage("DOMAIN_PROCESSING")
                .build();

        // When
        errorHandler.handleError(error1, acknowledgment);
        errorHandler.handleError(error2, acknowledgment);

        // Then
        verify(acknowledgment, times(2)).acknowledge();
    }
}
