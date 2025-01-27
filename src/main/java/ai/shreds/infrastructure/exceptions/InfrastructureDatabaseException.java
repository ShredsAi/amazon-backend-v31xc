package ai.shreds.infrastructure.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InfrastructureDatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    private final String errorCode;
    private final LocalDateTime timestamp;
    private final String operation;
    private final String details;

    public InfrastructureDatabaseException(String message, String operation) {
        super(message);
        this.errorCode = "DB_ERROR";
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.details = null;
    }

    public InfrastructureDatabaseException(String message, String operation, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.details = null;
    }

    public InfrastructureDatabaseException(String message, String operation, String errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.details = details;
    }

    public InfrastructureDatabaseException(String message, String operation, Throwable cause) {
        super(message, cause);
        this.errorCode = "DB_ERROR";
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.details = cause.getMessage();
    }

    public InfrastructureDatabaseException(String message, String operation, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.details = cause.getMessage();
    }

    @Override
    public String toString() {
        return String.format("DatabaseException[errorCode=%s, timestamp=%s, operation=%s, message=%s, details=%s]",
                errorCode, timestamp, operation, getMessage(), details);
    }
}
