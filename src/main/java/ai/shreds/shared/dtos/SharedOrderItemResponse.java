package ai.shreds.shared.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Response object containing order item details")
public class SharedOrderItemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the product ordered", example = "1001")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the product ordered", example = "2", minimum = "1")
    private Integer quantity;

    @NotNull(message = "Price is required")
    @Schema(description = "Price per unit of the product", example = "29.99")
    private BigDecimal price;

    public SharedOrderItemResponse() {
    }

    public SharedOrderItemResponse(Long productId, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "SharedOrderItemResponse{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}