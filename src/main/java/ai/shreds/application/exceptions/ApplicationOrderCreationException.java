package ai.shreds.application.exceptions;

import java.time.LocalDateTime;

public class ApplicationOrderCreationException extends RuntimeException {

    private final String message;
    private final String errorCode;
    private final LocalDateTime timestamp;
    private final String technicalDetails;

    public ApplicationOrderCreationException(String message, String errorCode) {
        this(message, errorCode, LocalDateTime.now(), null, null);
    }

    public ApplicationOrderCreationException(String message, String errorCode, LocalDateTime timestamp) {
        this(message, errorCode, timestamp, null, null);
    }

    public ApplicationOrderCreationException(String message, String errorCode, Throwable cause) {
        this(message, errorCode, LocalDateTime.now(), cause.getMessage(), cause);
    }

    public ApplicationOrderCreationException(String message, String errorCode, LocalDateTime timestamp, String technicalDetails) {
        this(message, errorCode, timestamp, technicalDetails, null);
    }

    public ApplicationOrderCreationException(String message, String errorCode, LocalDateTime timestamp, String technicalDetails, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
        this.technicalDetails = technicalDetails;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    public boolean hasTechnicalDetails() {
        return technicalDetails != null && !technicalDetails.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append("ApplicationOrderCreationException{")
            .append("message='").append(message).append('\\'')
            .append(", errorCode='").append(errorCode).append('\\'')
            .append(", timestamp=").append(timestamp);
        
        if (hasTechnicalDetails()) {
            sb.append(", technicalDetails='").append(technicalDetails).append('\\'');
        }
        
        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName())
              .append("('").append(getCause().getMessage()).append("')");
        }
        
        return sb.append('}').toString();
    }
}
