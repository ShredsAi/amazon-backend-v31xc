package ai.shreds.shared.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Response object containing order details")
public class SharedOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Unique identifier of the order", example = "1001")
    private Long id;

    @Schema(description = "ID of the user who placed the order", example = "123")
    private Long userId;

    @Schema(description = "Total amount of the order", example = "99.99")
    private BigDecimal totalAmount;

    @Schema(description = "Current payment status", example = "SUCCESS", allowableValues = {"PENDING", "SUCCESS", "FAILED", "DECLINED"})
    private String paymentStatus;

    @Schema(description = "Current reservation state", example = "RESERVED", allowableValues = {"PENDING", "RESERVED", "COMPLETED", "CANCELLED"})
    private String reservationState;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @Schema(description = "Order creation timestamp", example = "2023-10-01T10:15:30.000+0000")
    private String createdAt;

    @Schema(description = "List of items in the order")
    private List<SharedOrderItemResponse> items;

    public SharedOrderResponse() {
    }

    public SharedOrderResponse(Long id, Long userId, BigDecimal totalAmount, String paymentStatus,
                             String reservationState, String createdAt, List<SharedOrderItemResponse> items) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.reservationState = reservationState;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getReservationState() {
        return reservationState;
    }

    public void setReservationState(String reservationState) {
        this.reservationState = reservationState;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<SharedOrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<SharedOrderItemResponse> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "SharedOrderResponse{" +
                "id=" + id +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", reservationState='" + reservationState + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", items=" + items +
                '}';
    }
}
