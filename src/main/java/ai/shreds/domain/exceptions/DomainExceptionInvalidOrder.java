package ai.shreds.domain.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when order validation fails due to business rule violations.
 * Extends DomainOrderException to maintain the domain exception hierarchy.
 */
public class DomainExceptionInvalidOrder extends DomainOrderException {

    private final List<ValidationError> validationErrors;

    /**
     * Represents a specific validation error.
     */
    public static class ValidationError {
        private final String field;
        private final String message;
        private final String code;

        public ValidationError(String field, String message, String code) {
            this.field = field;
            this.message = message;
            this.code = code;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return String.format("%s: %s (code: %s)", field, message, code);
        }
    }

    /**
     * Constructs a new invalid order exception with a single error message.
     *
     * @param message The error message
     */
    public DomainExceptionInvalidOrder(String message) {
        super(message, "ORDER_INVALID");
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(new ValidationError("order", message, "GENERAL"));
    }

    /**
     * Constructs a new invalid order exception with a specific validation error.
     *
     * @param field The field that failed validation
     * @param message The error message
     * @param code The specific error code
     */
    public DomainExceptionInvalidOrder(String field, String message, String code) {
        super(message, "ORDER_INVALID");
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(new ValidationError(field, message, code));
    }

    /**
     * Constructs a new invalid order exception with multiple validation errors.
     *
     * @param errors List of validation errors
     */
    public DomainExceptionInvalidOrder(List<ValidationError> errors) {
        super("Multiple validation errors occurred", "ORDER_INVALID_MULTIPLE");
        this.validationErrors = new ArrayList<>(errors);
    }

    /**
     * Creates a builder for constructing an exception with multiple errors.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the list of validation errors.
     *
     * @return Unmodifiable list of validation errors
     */
    public List<ValidationError> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    /**
     * Builder class for constructing exceptions with multiple validation errors.
     */
    public static class Builder {
        private final List<ValidationError> errors = new ArrayList<>();

        public Builder addError(String field, String message, String code) {
            errors.add(new ValidationError(field, message, code));
            return this;
        }

        public DomainExceptionInvalidOrder build() {
            if (errors.isEmpty()) {
                throw new IllegalStateException("At least one validation error is required");
            }
            return new DomainExceptionInvalidOrder(errors);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("
Validation Errors:");
        validationErrors.forEach(error -> 
            sb.append("
- ").append(error.toString()));
        return sb.toString();
    }
}
