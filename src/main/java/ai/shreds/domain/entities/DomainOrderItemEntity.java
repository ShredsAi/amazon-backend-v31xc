package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.domain.value_objects.DomainValueMoney;

import java.util.Objects;

/**
 * Domain entity representing an item within an order.
 * Contains product details, quantity, and price information.
 */
public class DomainOrderItemEntity {

    private Long id;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private DomainValueMoney price;

    protected DomainOrderItemEntity() {
    }

    public DomainOrderItemEntity(Long productId, Integer quantity, DomainValueMoney price) {
        validateProductId(productId);
        validateQuantity(quantity);
        validatePrice(price);

        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * Calculates the subtotal for this item.
     *
     * @return The total price for this item (price * quantity)
     */
    public DomainValueMoney calculateSubtotal() {
        return price.multiply(quantity);
    }

    /**
     * Updates the quantity of this item.
     *
     * @param newQuantity The new quantity
     * @throws DomainExceptionInvalidOrder if quantity is invalid
     */
    public void updateQuantity(Integer newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    /**
     * Updates the price of this item.
     *
     * @param newPrice The new price
     * @throws DomainExceptionInvalidOrder if price is invalid
     */
    public void updatePrice(DomainValueMoney newPrice) {
        validatePrice(newPrice);
        this.price = newPrice;
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new DomainExceptionInvalidOrder("Product ID must be a positive number");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new DomainExceptionInvalidOrder("Quantity must be a positive number");
        }
    }

    private void validatePrice(DomainValueMoney price) {
        if (price == null || price.isZero()) {
            throw new DomainExceptionInvalidOrder("Price must be greater than zero");
        }
    }

    /**
     * Validates the entire entity state.
     *
     * @throws DomainExceptionInvalidOrder if any validation fails
     */
    public void validate() {
        validateProductId(this.productId);
        validateQuantity(this.quantity);
        validatePrice(this.price);

        if (this.orderId != null && this.orderId <= 0) {
            throw new DomainExceptionInvalidOrder("Order ID must be a positive number");
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public DomainValueMoney getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainOrderItemEntity that = (DomainOrderItemEntity) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(orderId, that.orderId) &&
               Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderId, productId);
    }

    @Override
    public String toString() {
        return String.format("OrderItem[id=%d, orderId=%d, productId=%d, quantity=%d, price=%s]",
            id, orderId, productId, quantity, price);
    }
}
