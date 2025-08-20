package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.domain.value_objects.DomainValueMoney;

import java.math.BigDecimal;
import java.util.Objects;

public class DomainEntityOrderItem {

    private Long id;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private DomainValueMoney price;

    public DomainEntityOrderItem() {
    }

    public DomainEntityOrderItem(Long productId, Integer quantity, DomainValueMoney price) {
        validateQuantity(quantity);
        validatePrice(price);
        validateProductId(productId);

        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public DomainValueMoney calculateSubtotal() {
        return DomainValueMoney.of(
            price.getAmount().multiply(new BigDecimal(quantity)),
            price.getCurrency()
        );
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new DomainExceptionInvalidOrder("Item quantity must be positive");
        }
    }

    private void validatePrice(DomainValueMoney price) {
        if (price == null || price.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainExceptionInvalidOrder("Item price must be positive");
        }
    }

    private void validateProductId(Long productId) {
        if (productId == null) {
            throw new DomainExceptionInvalidOrder("Product ID cannot be null");
        }
    }

    public void updateQuantity(Integer newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    public void updatePrice(DomainValueMoney newPrice) {
        validatePrice(newPrice);
        this.price = newPrice;
    }

    public void validate() {
        validateQuantity(this.quantity);
        validatePrice(this.price);
        validateProductId(this.productId);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        validateProductId(productId);
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    public DomainValueMoney getPrice() {
        return price;
    }

    public void setPrice(DomainValueMoney price) {
        validatePrice(price);
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEntityOrderItem that = (DomainEntityOrderItem) o;
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
        return "DomainEntityOrderItem{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}
