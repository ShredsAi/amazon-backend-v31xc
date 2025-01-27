package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationInputPortCreateOrder;
import ai.shreds.domain.entities.DomainEntityOrder;
import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.domain.value_objects.DomainValueOrderStatus;
import ai.shreds.domain.ports.DomainInputPortCreateOrder;
import ai.shreds.shared.dtos.SharedCreateOrderRequest;
import ai.shreds.shared.dtos.SharedOrderResponse;
import ai.shreds.shared.dtos.SharedOrderItemResponse;
import ai.shreds.domain.exceptions.*;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ai.shreds.application.exceptions.ApplicationOrderValidationException;
import ai.shreds.application.exceptions.ApplicationOrderCreationException;

@Service
public class ApplicationServiceCreateOrder implements ApplicationInputPortCreateOrder {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceCreateOrder.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final DomainInputPortCreateOrder domainCreateOrder;

    public ApplicationServiceCreateOrder(DomainInputPortCreateOrder domainCreateOrder) {
        this.domainCreateOrder = domainCreateOrder;
    }

    @Override
    @Transactional
    public SharedOrderResponse createOrder(SharedCreateOrderRequest request) {
        logger.debug("Creating order for user: {}", request.getUserId());
        
        try {
            validateRequest(request);
            DomainEntityOrder domainOrder = mapRequestToDomain(request);
            DomainEntityOrder savedOrder = domainCreateOrder.execute(domainOrder);
            
            logger.info("Successfully created order with ID: {} for user: {}", 
                       savedOrder.getId(), savedOrder.getUserId());
            
            return mapDomainToShared(savedOrder);
            
        } catch (DomainOrderException | DomainInventoryException | 
                 DomainPaymentException | DomainExceptionInvalidOrder | 
                 DomainExceptionPaymentFailed e) {
            logger.error("Failed to create order for user {}: {}", request.getUserId(), e.getMessage());
            throw new ApplicationOrderCreationException(
                String.format("Failed to create order: %s", e.getMessage()),
                "ERR-ORDER-CREATION",
                LocalDateTime.now()
            );
        } catch (Exception e) {
            logger.error("Unexpected error while creating order for user {}: {}", 
                        request.getUserId(), e.getMessage());
            throw new ApplicationOrderCreationException(
                "An unexpected error occurred while creating the order",
                "ERR-UNEXPECTED",
                LocalDateTime.now()
            );
        }
    }

    private void validateRequest(SharedCreateOrderRequest request) {
        List<String> validationErrors = new ArrayList<>();

        if (request == null) {
            throw new ApplicationOrderValidationException(
                "Order request cannot be null",
                "ERR-400",
                LocalDateTime.now()
            );
        }

        if (request.getUserId() == null) {
            validationErrors.add("userId is required");
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            validationErrors.add("paymentMethod is required");
        } else if (!isValidPaymentMethod(request.getPaymentMethod())) {
            validationErrors.add("Invalid payment method: " + request.getPaymentMethod());
        }

        if (!validationErrors.isEmpty()) {
            throw new ApplicationOrderValidationException(
                String.join(", ", validationErrors),
                "ERR-400",
                LocalDateTime.now()
            );
        }
    }

    private boolean isValidPaymentMethod(String paymentMethod) {
        return paymentMethod.matches("^(CREDIT_CARD|DEBIT_CARD|PAYPAL)$");
    }

    private DomainEntityOrder mapRequestToDomain(SharedCreateOrderRequest request) {
        DomainEntityOrder order = new DomainEntityOrder();
        order.setUserId(request.getUserId());
        order.setCreatedAt(LocalDateTime.now());
        order.setPaymentStatus(new DomainValuePaymentStatus(SharedPaymentStatusEnum.PENDING));
        order.setReservationState(new DomainValueOrderStatus(SharedOrderStatusEnum.PENDING));
        
        // Note: The actual items will be retrieved from the cart service by the domain layer
        // The domain layer will handle the cart retrieval, inventory check, and price calculation
        return order;
    }

    private SharedOrderResponse mapDomainToShared(DomainEntityOrder domainOrder) {
        SharedOrderResponse response = new SharedOrderResponse();
        response.setId(domainOrder.getId());
        response.setUserId(domainOrder.getUserId());
        response.setTotalAmount(domainOrder.getTotalAmount().getAmount());
        response.setPaymentStatus(domainOrder.getPaymentStatus().getStatus().name());
        response.setReservationState(domainOrder.getReservationState().getStatus().name());
        response.setCreatedAt(domainOrder.getCreatedAt().format(DATE_FORMATTER));
        
        List<SharedOrderItemResponse> items = domainOrder.getItems().stream()
            .map(this::mapDomainItemToShared)
            .collect(Collectors.toList());
        response.setItems(items);
        
        return response;
    }

    private SharedOrderItemResponse mapDomainItemToShared(DomainEntityOrderItem domainItem) {
        return new SharedOrderItemResponse(
            domainItem.getProductId(),
            domainItem.getQuantity(),
            domainItem.getPrice().getAmount()
        );
    }
}
