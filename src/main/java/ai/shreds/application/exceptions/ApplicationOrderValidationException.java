package ai.shreds.application.exceptions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApplicationOrderValidationException extends RuntimeException {

    private final String message;
    private final String errorCode;
    private final LocalDateTime timestamp;
    private final List<String> validationErrors;

    public ApplicationOrderValidationException(String message, String errorCode) {
        this(message, errorCode, LocalDateTime.now(), Collections.emptyList());
    }

    public ApplicationOrderValidationException(String message, String errorCode, LocalDateTime timestamp) {
        this(message, errorCode, timestamp, Collections.emptyList());
    }

    public ApplicationOrderValidationException(String message, String errorCode, List<String> validationErrors) {
        this(message, errorCode, LocalDateTime.now(), validationErrors);
    }

    public ApplicationOrderValidationException(String message, String errorCode, LocalDateTime timestamp, List<String> validationErrors) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
        this.validationErrors = new ArrayList<>(validationErrors);
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

    public List<String> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append("ApplicationOrderValidationException{")
            .append("message='").append(message).append("'")
            .append(", errorCode='").append(errorCode).append("'")
            .append(", timestamp=").append(timestamp);

        if (!validationErrors.isEmpty()) {
            sb.append(", validationErrors=").append(validationErrors);
        }

        return sb.append('}').toString();
    }
}
