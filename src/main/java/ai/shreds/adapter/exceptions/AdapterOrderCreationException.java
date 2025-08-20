package ai.shreds.adapter.exceptions;

import java.time.LocalDateTime;

public class AdapterOrderCreationException extends RuntimeException {

    private final String errorCode;
    private final LocalDateTime timestamp;

    public AdapterOrderCreationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
