package ai.shreds.domain.services;

import ai.shreds.domain.entities.*;
import ai.shreds.domain.exceptions.*;
import ai.shreds.domain.value_objects.*;
import ai.shreds.domain.ports.*;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DomainOrderService implements DomainInputPortCreateOrder {

    private final DomainOutputPortOrderRepository orderRepository;
    private final DomainOutputPortInventoryService inventoryService;
    private final DomainOutputPortPaymentService paymentService;

    public DomainOrderService(DomainOutputPortOrderRepository orderRepository,
                             DomainOutputPortInventoryService inventoryService,
                             DomainOutputPortPaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    @Override
    public DomainEntityOrder execute(DomainEntityOrder order) {
        try {
            // Initial validation
            validateOrder(order);

            // Set initial state
            order.setCreatedAt(LocalDateTime.now());
            order.setReservationState(DomainValueOrderStatus.initial());
            order.setPaymentStatus(DomainValuePaymentStatus.initial());

            // Calculate total amount
            validateAndCalculateTotalAmount(order);

            // Reserve inventory
            reserveInventory(order);

            // Process payment
            processPayment(order);

            // Save order and items
            DomainEntityOrder savedOrder = orderRepository.save(order);
            if (!order.getItems().isEmpty()) {
                order.getItems().forEach(item -> item.setOrderId(savedOrder.getId()));
                orderRepository.saveItems(order.getItems());
            }

            return savedOrder;

        } catch (DomainExceptionPaymentFailed e) {
            // Handle specific payment failures
            throw e;
        } catch (DomainPaymentException e) {
            // Handle general payment errors
            throw e;
        } catch (DomainExceptionInvalidOrder | DomainInventoryException e) {
            // Handle validation and inventory errors
            throw e;
        } catch (Exception e) {
            throw new DomainOrderException("Unexpected error during order processing: " + e.getMessage(), 
                "ORDER_PROCESSING_ERROR");
        }
    }

    private void validateOrder(DomainEntityOrder order) {
        if (order == null) {
            throw new DomainExceptionInvalidOrder("Order cannot be null");
        }

        if (order.getUserId() == null) {
            throw new DomainExceptionInvalidOrder("User ID is required");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new DomainExceptionInvalidOrder("Order must contain at least one item");
        }

        order.getItems().forEach(this::validateOrderItem);
    }

    private void validateOrderItem(DomainEntityOrderItem item) {
        if (item == null) {
            throw new DomainExceptionInvalidOrder("Order item cannot be null");
        }

        if (item.getProductId() == null) {
            throw new DomainExceptionInvalidOrder("Product ID is required");
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new DomainExceptionInvalidOrder(
                String.format("Invalid quantity for product ID %d", item.getProductId())
            );
        }

        if (item.getPrice() == null || item.getPrice().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainExceptionInvalidOrder(
                String.format("Invalid price for product ID %d", item.getProductId())
            );
        }
    }

    private void validateAndCalculateTotalAmount(DomainEntityOrder order) {
        DomainValueMoney total = order.getItems().stream()
            .map(item -> item.getPrice().multiply(item.getQuantity()))
            .reduce(DomainValueMoney.zero("USD"), DomainValueMoney::add);

        order.setTotalAmount(total);
    }

    private void reserveInventory(DomainEntityOrder order) {
        try {
            boolean reserved = inventoryService.reserveItems(order.getItems());
            if (!reserved) {
                throw new DomainInventoryException("Failed to reserve inventory");
            }
            order.setReservationState(order.getReservationState()
                .transition(SharedOrderStatusEnum.RESERVED));
        } catch (Exception e) {
            throw new DomainInventoryException("Inventory reservation failed: " + e.getMessage());
        }
    }

    private void processPayment(DomainEntityOrder order) {
        DomainEntityPaymentDetails paymentDetails = new DomainEntityPaymentDetails(
            order.getId(),
            order.getPaymentMethod(),
            order.getTotalAmount()
        );

        try {
            DomainEntityPaymentDetails processedPayment = paymentService.processPayment(paymentDetails);
            handlePaymentResult(order, processedPayment);
        } catch (Exception e) {
            handlePaymentFailure(order, e);
        }
    }

    private void handlePaymentResult(DomainEntityOrder order, DomainEntityPaymentDetails processedPayment) {
        if (processedPayment == null || processedPayment.getStatus() == null) {
            handlePaymentFailure(order, new DomainPaymentException("Invalid payment response"));
            return;
        }

        SharedPaymentStatusEnum paymentStatus = processedPayment.getStatus().getStatus();
        switch (paymentStatus) {
            case SUCCESS:
                order.updatePaymentStatus(SharedPaymentStatusEnum.SUCCESS);
                order.updateReservationState(SharedOrderStatusEnum.COMPLETED);
                break;
            case FAILED:
            case DECLINED:
                handlePaymentFailure(order, 
                    DomainExceptionPaymentFailed.newBuilder()
                        .message("Payment " + paymentStatus.toString().toLowerCase())
                        .reason(DomainExceptionPaymentFailed.FailureReason.CARD_DECLINED)
                        .build());
                break;
            default:
                handlePaymentFailure(order, 
                    DomainExceptionPaymentFailed.newBuilder()
                        .message("Unexpected payment status: " + paymentStatus)
                        .reason(DomainExceptionPaymentFailed.FailureReason.UNKNOWN)
                        .build());
        }
    }

    private void handlePaymentFailure(DomainEntityOrder order, Exception e) {
        try {
            // Release inventory
            inventoryService.releaseItems(order.getItems());
            // Update order status
            order.updatePaymentStatus(SharedPaymentStatusEnum.FAILED);
            order.updateReservationState(SharedOrderStatusEnum.CANCELLED);
        } catch (Exception releaseException) {
            // Log the release exception but throw the original payment exception
            throw new DomainPaymentException("Payment failed and inventory release failed: " + 
                e.getMessage() + ". Release error: " + releaseException.getMessage());
        }
        
        if (e instanceof DomainExceptionPaymentFailed) {
            throw (DomainExceptionPaymentFailed) e;
        } else {
            throw DomainExceptionPaymentFailed.newBuilder()
                .message(e.getMessage())
                .reason(DomainExceptionPaymentFailed.FailureReason.TECHNICAL_ERROR)
                .build();
        }
    }
}