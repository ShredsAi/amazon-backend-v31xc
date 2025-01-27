package ai.shreds.domain.exceptions;

import java.time.LocalDateTime;

/**
 * Base exception class for all order-related domain exceptions.
 * Provides common functionality for handling domain-level order errors.
 */
public class DomainOrderException extends RuntimeException {

    private final String errorCode;
    private final LocalDateTime timestamp;
    private final String domain;

    /**
     * Constructs a new domain order exception.
     *
     * @param message Detailed error message
     * @param errorCode Specific error code for this exception
     */
    public DomainOrderException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.domain = "ORDER";
    }

    /**
     * Constructs a new domain order exception with a cause.
     *
     * @param message Detailed error message
     * @param errorCode Specific error code for this exception
     * @param cause The cause of this exception
     */
    public DomainOrderException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.domain = "ORDER";
    }

    /**
     * Creates a new instance with formatted message.
     *
     * @param message Message format string
     * @param errorCode Specific error code
     * @param args Message format arguments
     * @return New DomainOrderException instance
     */
    public static DomainOrderException of(String message, String errorCode, Object... args) {
        return new DomainOrderException(
            String.format(message, args),
            errorCode
        );
    }

    /**
     * Creates a new instance with formatted message and cause.
     *
     * @param message Message format string
     * @param errorCode Specific error code
     * @param cause The cause of this exception
     * @param args Message format arguments
     * @return New DomainOrderException instance
     */
    public static DomainOrderException of(String message, String errorCode, Throwable cause, Object... args) {
        return new DomainOrderException(
            String.format(message, args),
            errorCode,
            cause
        );
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return The error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the timestamp when this exception was created.
     *
     * @return The creation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the domain this exception belongs to.
     *
     * @return The domain name
     */
    public String getDomain() {
        return domain;
    }

    @Override
    public String toString() {
        return String.format("%s[%s] at %s: %s", 
            domain, errorCode, timestamp, getMessage());
    }
}
