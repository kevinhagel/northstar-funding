package com.northstar.funding.workflow.kafka;

import com.northstar.funding.kafka.events.WorkflowErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for workflow-errors topic.
 *
 * <p>Acts as a dead letter queue handler for workflow errors.
 * Logs errors for monitoring and debugging.
 *
 * <p>Configuration:
 * <ul>
 *   <li>Topic: workflow-errors</li>
 *   <li>Group ID: error-handler</li>
 *   <li>Manual acknowledgment</li>
 *   <li>Future: Could persist to database, send alerts, etc.</li>
 * </ul>
 */
@Service
public class WorkflowErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowErrorHandler.class);

    @KafkaListener(
            topics = "workflow-errors",
            groupId = "error-handler",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleError(WorkflowErrorEvent event, Acknowledgment acknowledgment) {
        try {
            if (event == null) {
                log.warn("⚠️ Received null error event");
                return;
            }

            // Log error with details
            log.error("❌ WORKFLOW ERROR | sessionId={} | type={} | stage={} | message={}",
                    event.getSessionId(),
                    event.getErrorType(),
                    event.getStage(),
                    event.getErrorMessage());

            // Log stack trace if available
            if (event.getStackTrace() != null && !event.getStackTrace().isEmpty()) {
                log.error("Stack trace:\n{}", event.getStackTrace());
            }

            // Future enhancements:
            // - Persist to error_log table for audit trail
            // - Send alerts for critical errors (via email, Slack, etc.)
            // - Track error frequency by type/component
            // - Implement retry logic for transient errors

        } catch (Exception e) {
            log.error("❌ Error handler itself failed: {}", e.getMessage(), e);

        } finally {
            acknowledgment.acknowledge();
        }
    }
}
