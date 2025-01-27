package ai.shreds.infrastructure.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InfrastructureServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    private final String serviceType;
    private final String errorCode;
    private final LocalDateTime timestamp;
    private final String operation;
    private final Integer statusCode;
    private final String details;

    public InfrastructureServiceException(String message, String serviceType, String operation) {
        super(message);
        this.serviceType = serviceType;
        this.errorCode = serviceType + "_ERROR";
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.statusCode = null;
        this.details = null;
    }

    public InfrastructureServiceException(String message, String serviceType, String operation, Integer statusCode) {
        super(message);
        this.serviceType = serviceType;
        this.errorCode = serviceType + "_ERROR_" + statusCode;
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.statusCode = statusCode;
        this.details = null;
    }

    public InfrastructureServiceException(String message, String serviceType, String operation, 
            Integer statusCode, String details) {
        super(message);
        this.serviceType = serviceType;
        this.errorCode = serviceType + "_ERROR_" + statusCode;
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.statusCode = statusCode;
        this.details = details;
    }

    public InfrastructureServiceException(String message, String serviceType, String operation, Throwable cause) {
        super(message, cause);
        this.serviceType = serviceType;
        this.errorCode = serviceType + "_ERROR";
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.statusCode = null;
        this.details = cause.getMessage();
    }

    public InfrastructureServiceException(String message, String serviceType, String operation, 
            Integer statusCode, Throwable cause) {
        super(message, cause);
        this.serviceType = serviceType;
        this.errorCode = serviceType + "_ERROR_" + statusCode;
        this.timestamp = LocalDateTime.now();
        this.operation = operation;
        this.statusCode = statusCode;
        this.details = cause.getMessage();
    }

    @Override
    public String toString() {
        return String.format("ServiceException[serviceType=%s, errorCode=%s, timestamp=%s, operation=%s, statusCode=%d, message=%s, details=%s]",
                serviceType, errorCode, timestamp, operation, statusCode, getMessage(), details);
    }

    public boolean isRetryable() {
        if (statusCode == null) return false;
        // Consider 5xx errors as retryable
        return statusCode >= 500 && statusCode < 600;
    }

    public static class Builder {
        private String message;
        private String serviceType;
        private String operation;
        private Integer statusCode;
        private String details;
        private Throwable cause;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder serviceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder statusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public InfrastructureServiceException build() {
            if (cause != null) {
                return new InfrastructureServiceException(message, serviceType, operation, statusCode, cause);
            } else if (details != null) {
                return new InfrastructureServiceException(message, serviceType, operation, statusCode, details);
            } else if (statusCode != null) {
                return new InfrastructureServiceException(message, serviceType, operation, statusCode);
            } else {
                return new InfrastructureServiceException(message, serviceType, operation);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
