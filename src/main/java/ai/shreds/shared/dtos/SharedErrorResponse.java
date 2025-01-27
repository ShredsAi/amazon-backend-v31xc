package ai.shreds.shared.dtos;

import java.time.LocalDateTime;

public class SharedErrorResponse {
    private final String message;
    private final String errorCode;
    private final LocalDateTime timestamp;
    private final String path;

    public SharedErrorResponse(String message, String errorCode, String path) {
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }
}