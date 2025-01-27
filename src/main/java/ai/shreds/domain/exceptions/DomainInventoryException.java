package ai.shreds.domain.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DomainInventoryException extends DomainOrderException {

    private final List<InventoryError> inventoryErrors;

    public static class InventoryError {
        private final Long productId;
        private final Integer requestedQuantity;
        private final Integer availableQuantity;
        private final String errorCode;
        private final String errorDetails;

        private InventoryError(Builder builder) {
            this.productId = builder.productId;
            this.requestedQuantity = builder.requestedQuantity;
            this.availableQuantity = builder.availableQuantity;
            this.errorCode = builder.errorCode;
            this.errorDetails = builder.errorDetails;
        }

        public Long getProductId() {
            return productId;
        }

        public Integer getRequestedQuantity() {
            return requestedQuantity;
        }

        public Integer getAvailableQuantity() {
            return availableQuantity;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorDetails() {
            return errorDetails;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long productId;
            private Integer requestedQuantity;
            private Integer availableQuantity;
            private String errorCode;
            private String errorDetails;

            public Builder productId(Long productId) {
                this.productId = productId;
                return this;
            }

            public Builder requestedQuantity(Integer requestedQuantity) {
                this.requestedQuantity = requestedQuantity;
                return this;
            }

            public Builder availableQuantity(Integer availableQuantity) {
                this.availableQuantity = availableQuantity;
                return this;
            }

            public Builder errorCode(String errorCode) {
                this.errorCode = errorCode;
                return this;
            }

            public Builder errorDetails(String errorDetails) {
                this.errorDetails = errorDetails;
                return this;
            }

            public InventoryError build() {
                return new InventoryError(this);
            }
        }

        @Override
        public String toString() {
            return String.format(
                "Product ID: %d, Requested: %d, Available: %d, Error: [%s] %s",
                productId, requestedQuantity, availableQuantity, errorCode, errorDetails
            );
        }
    }

    public DomainInventoryException(String message) {
        super(message, "INVENTORY_ERROR");
        this.inventoryErrors = new ArrayList<>();
    }

    public DomainInventoryException(String message, InventoryError inventoryError) {
        super(message, "INVENTORY_ERROR_" + inventoryError.getErrorCode());
        this.inventoryErrors = new ArrayList<>();
        this.inventoryErrors.add(inventoryError);
    }

    public DomainInventoryException(String message, List<InventoryError> errors) {
        super(message, "INVENTORY_ERROR_MULTIPLE");
        this.inventoryErrors = new ArrayList<>(errors);
    }

    public List<InventoryError> getInventoryErrors() {
        return Collections.unmodifiableList(inventoryErrors);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(super.toString());
        if (!inventoryErrors.isEmpty()) {
            str.append(System.lineSeparator()).append("Inventory Errors:");
            for (InventoryError error : inventoryErrors) {
                str.append(System.lineSeparator()).append("- ").append(error.toString());
            }
        }
        return str.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private final List<InventoryError> errors = new ArrayList<>();

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder addError(InventoryError error) {
            errors.add(error);
            return this;
        }

        public Builder addError(Long productId, Integer requested, Integer available,
                              String errorCode, String details) {
            errors.add(InventoryError.builder()
                .productId(productId)
                .requestedQuantity(requested)
                .availableQuantity(available)
                .errorCode(errorCode)
                .errorDetails(details)
                .build());
            return this;
        }

        public DomainInventoryException build() {
            if (errors.isEmpty()) {
                return new DomainInventoryException(message);
            } else if (errors.size() == 1) {
                return new DomainInventoryException(message, errors.get(0));
            } else {
                return new DomainInventoryException(message, errors);
            }
        }
    }
}