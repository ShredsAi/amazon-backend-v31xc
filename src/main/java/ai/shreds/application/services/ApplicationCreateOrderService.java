package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationCreateOrderInputPort;
import ai.shreds.domain.entities.DomainEntityOrder;
import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.entities.DomainEntityPaymentDetails;
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
import ai.shreds.application.exceptions.ApplicationOrderValidationException;
import ai.shreds.application.exceptions.ApplicationOrderCreationException;

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

/**
 * Service implementation for creating orders in the application layer.
 * Handles the orchestration of order creation, including validation,
 * domain entity creation, and response mapping.
 */
@Service
public class ApplicationCreateOrderService implements ApplicationCreateOrderInputPort {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationCreateOrderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final DomainInputPortCreateOrder domainOrderCreator;

    public ApplicationCreateOrderService(DomainInputPortCreateOrder domainOrderCreator) {
        this.domainOrderCreator = domainOrderCreator;
    }

    @Override
    @Transactional
    public SharedOrderResponse createOrder(SharedCreateOrderRequest request) {
        logger.debug("Starting order creation process for user: {}", request.getUserId());
        
        try {
            // Validate the request
            validateOrderRequest(request);

            // Create and prepare the domain order
            DomainEntityOrder domainOrder = createDomainOrder(request);
            
            // Execute the order creation through the domain layer
            DomainEntityOrder createdOrder = domainOrderCreator.execute(domainOrder);
            
            logger.info("Successfully created order with ID: {} for user: {}", 
                       createdOrder.getId(), createdOrder.getUserId());
            
            // Map and return the response
            return mapToSharedOrderResponse(createdOrder);
            
        } catch (DomainOrderException | DomainInventoryException | 
                 DomainPaymentException | DomainExceptionInvalidOrder | 
                 DomainExceptionPaymentFailed e) {
            logger.error("Domain error during order creation for user {}: {}", 
                        request.getUserId(), e.getMessage());
            throw new ApplicationOrderCreationException(
                String.format("Failed to create order: %s", e.getMessage()),
                "ERR-ORDER-CREATION",
                LocalDateTime.now(),
                e.getMessage(),
                e
            );
        } catch (Exception e) {
            logger.error("Unexpected error during order creation for user {}: {}", 
                        request.getUserId(), e.getMessage(), e);
            throw new ApplicationOrderCreationException(
                "An unexpected error occurred while creating the order",
                "ERR-UNEXPECTED",
                LocalDateTime.now(),
                e.getMessage(),
                e
            );
        }
    }

    private DomainEntityOrder createDomainOrder(SharedCreateOrderRequest request) {
        DomainEntityOrder order = new DomainEntityOrder();
        order.setUserId(request.getUserId());
        order.setCreatedAt(LocalDateTime.now());
        
        // Set initial status
        order.setPaymentStatus(new DomainValuePaymentStatus(SharedPaymentStatusEnum.PENDING));
        order.setReservationState(new DomainValueOrderStatus(SharedOrderStatusEnum.PENDING));
        
        // Create payment details
        DomainEntityPaymentDetails paymentDetails = new DomainEntityPaymentDetails();
        paymentDetails.setPaymentMethod(request.getPaymentMethod());
        
        logger.debug("Created domain order for user: {} with payment method: {}", 
                    request.getUserId(), request.getPaymentMethod());
        
        return order;
    }

    private SharedOrderResponse mapToSharedOrderResponse(DomainEntityOrder domainOrder) {
        SharedOrderResponse response = new SharedOrderResponse();
        response.setId(domainOrder.getId());
        response.setUserId(domainOrder.getUserId());
        response.setTotalAmount(domainOrder.getTotalAmount().getAmount());
        response.setPaymentStatus(domainOrder.getPaymentStatus().getStatus().name());
        response.setReservationState(domainOrder.getReservationState().getStatus().name());
        response.setCreatedAt(domainOrder.getCreatedAt().format(DATE_FORMATTER));
        
        List<SharedOrderItemResponse> items = domainOrder.getItems().stream()
            .map(this::mapToSharedOrderItemResponse)
            .collect(Collectors.toList());
        response.setItems(items);
        
        logger.debug("Mapped domain order {} to response with {} items", 
                    domainOrder.getId(), items.size());
        
        return response;
    }

    private SharedOrderItemResponse mapToSharedOrderItemResponse(DomainEntityOrderItem item) {
        return new SharedOrderItemResponse(
            item.getProductId(),
            item.getQuantity(),
            item.getPrice().getAmount()
        );
    }
}
