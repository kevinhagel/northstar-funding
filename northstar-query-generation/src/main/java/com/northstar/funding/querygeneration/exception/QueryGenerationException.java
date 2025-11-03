package com.northstar.funding.querygeneration.exception;

/**
 * Exception thrown when query generation fails.
 *
 * <p>This exception wraps underlying failures from LM Studio,
 * network issues, parsing errors, or timeout exceptions.
 */
public class QueryGenerationException extends RuntimeException {

    public QueryGenerationException(String message) {
        super(message);
    }

    public QueryGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryGenerationException(Throwable cause) {
        super(cause);
    }
}
