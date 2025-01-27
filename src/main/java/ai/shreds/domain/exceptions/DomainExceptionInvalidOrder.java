package ai.shreds.domain.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DomainExceptionInvalidOrder extends DomainOrderException {

    private final List<ValidationError> validationErrors;

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

    public DomainExceptionInvalidOrder(String message) {
        super(message, "ORDER_INVALID");
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(new ValidationError("order", message, "GENERAL"));
    }

    public DomainExceptionInvalidOrder(String field, String message, String code) {
        super(message, "ORDER_INVALID");
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(new ValidationError(field, message, code));
    }

    public DomainExceptionInvalidOrder(List<ValidationError> errors) {
        super("Multiple validation errors occurred", "ORDER_INVALID_MULTIPLE");
        this.validationErrors = new ArrayList<>(errors);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ValidationError> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

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
        StringBuilder str = new StringBuilder(super.toString());
        if (!validationErrors.isEmpty()) {
            str.append(System.lineSeparator()).append("Validation Errors:");
            for (ValidationError error : validationErrors) {
                str.append(System.lineSeparator()).append("- ").append(error.toString());
            }
        }
        return str.toString();
    }
}