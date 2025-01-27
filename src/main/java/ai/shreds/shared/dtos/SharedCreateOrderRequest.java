package ai.shreds.shared.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "Request object for creating a new order")
public class SharedCreateOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "User ID is required")
    @Schema(description = "ID of the user placing the order", example = "123")
    private Long userId;

    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "^(CREDIT_CARD|DEBIT_CARD|PAYPAL)$", message = "Invalid payment method")
    @Schema(description = "Payment method to be used", example = "CREDIT_CARD", allowableValues = {"CREDIT_CARD", "DEBIT_CARD", "PAYPAL"})
    private String paymentMethod;

    public SharedCreateOrderRequest() {
    }

    public SharedCreateOrderRequest(Long userId, String paymentMethod) {
        this.userId = userId;
        this.paymentMethod = paymentMethod;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String toString() {
        return "SharedCreateOrderRequest{" +
                "userId=" + userId +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}
