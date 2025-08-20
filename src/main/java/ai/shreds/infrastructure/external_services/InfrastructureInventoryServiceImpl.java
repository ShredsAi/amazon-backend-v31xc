package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.ports.DomainOutputPortInventoryService;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceException;
import ai.shreds.infrastructure.external_services.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InfrastructureInventoryServiceImpl implements DomainOutputPortInventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureInventoryServiceImpl.class);

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InfrastructureInventoryServiceImpl(
            RestTemplate restTemplate,
            @Value("${inventory.service.url}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    @Override
    @Retryable(value = {InfrastructureServiceException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public boolean reserveItems(List<DomainEntityOrderItem> items) {
        try {
            logger.debug("Attempting to reserve {} items", items.size());
            String orderId = UUID.randomUUID().toString();

            List<ReservationItem> reservationItems = items.stream()
                .map(item -> ReservationItem.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build())
                .collect(Collectors.toList());

            ReservationRequest request = ReservationRequest.builder()
                .orderId(orderId)
                .items(reservationItems)
                .build();

            ResponseEntity<ReservationResponse> response = restTemplate.postForEntity(
                    inventoryServiceUrl + "/api/inventory/reserve",
                    request,
                    ReservationResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ReservationResponse reservationResponse = response.getBody();
                if (reservationResponse.isSuccess()) {
                    logger.info("Successfully reserved {} items with reservation ID: {}", 
                        items.size(), reservationResponse.getReservationId());
                    return true;
                } else {
                    logger.error("Failed to reserve items: {}", reservationResponse.getMessage());
                    return false;
                }
            }
            return false;

        } catch (Exception e) {
            logger.error("Error during inventory reservation", e);
            throw new InfrastructureServiceException("Failed to reserve inventory", "INVENTORY", "RESERVE_ITEMS", e);
        }
    }

    @Override
    @Retryable(value = {InfrastructureServiceException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void releaseItems(List<DomainEntityOrderItem> items) {
        try {
            logger.debug("Attempting to release {} items", items.size());
            String orderId = UUID.randomUUID().toString();

            List<ReservationItem> reservationItems = items.stream()
                .map(item -> ReservationItem.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build())
                .collect(Collectors.toList());

            ReleaseRequest request = ReleaseRequest.builder()
                .orderId(orderId)
                .items(reservationItems)
                .build();

            ResponseEntity<ReleaseResponse> response = restTemplate.postForEntity(
                    inventoryServiceUrl + "/api/inventory/release",
                    request,
                    ReleaseResponse.class);

            if (response.getStatusCode() != HttpStatus.OK || 
                response.getBody() == null || 
                !response.getBody().isSuccess()) {
                throw new InfrastructureServiceException(
                    "Failed to release inventory", 
                    "INVENTORY", 
                    "RELEASE_ITEMS"
                );
            }

            logger.info("Successfully released {} items", items.size());

        } catch (Exception e) {
            logger.error("Error during inventory release", e);
            throw new InfrastructureServiceException("Failed to release inventory", "INVENTORY", "RELEASE_ITEMS", e);
        }
    }

    @Override
    public Map<Long, Boolean> checkAvailability(List<DomainEntityOrderItem> items) {
        try {
            List<ReservationItem> checkItems = items.stream()
                .map(item -> ReservationItem.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build())
                .collect(Collectors.toList());

            ResponseEntity<Map<Long, Boolean>> response = restTemplate.exchange(
                    inventoryServiceUrl + "/api/inventory/check-availability",
                    HttpMethod.POST,
                    new HttpEntity<>(checkItems),
                    new ParameterizedTypeReference<Map<Long, Boolean>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            throw new InfrastructureServiceException(
                "Failed to check inventory availability",
                "INVENTORY",
                "CHECK_AVAILABILITY"
            );
        } catch (Exception e) {
            logger.error("Error checking inventory availability", e);
            throw new InfrastructureServiceException(
                "Failed to check inventory availability",
                "INVENTORY",
                "CHECK_AVAILABILITY",
                e
            );
        }
    }

    @Override
    public Map<Long, Integer> getInventoryLevels(List<Long> productIds) {
        try {
            ResponseEntity<Map<Long, Integer>> response = restTemplate.exchange(
                    inventoryServiceUrl + "/api/inventory/levels",
                    HttpMethod.POST,
                    new HttpEntity<>(productIds),
                    new ParameterizedTypeReference<Map<Long, Integer>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            throw new InfrastructureServiceException(
                "Failed to get inventory levels",
                "INVENTORY",
                "GET_LEVELS"
            );
        } catch (Exception e) {
            logger.error("Error getting inventory levels", e);
            throw new InfrastructureServiceException(
                "Failed to get inventory levels",
                "INVENTORY",
                "GET_LEVELS",
                e
            );
        }
    }

    @Override
    public boolean validateProducts(List<DomainEntityOrderItem> items) {
        try {
            List<Long> productIds = items.stream()
                .map(DomainEntityOrderItem::getProductId)
                .collect(Collectors.toList());

            ResponseEntity<Map<Long, Boolean>> response = restTemplate.exchange(
                    inventoryServiceUrl + "/api/inventory/validate-products",
                    HttpMethod.POST,
                    new HttpEntity<>(productIds),
                    new ParameterizedTypeReference<Map<Long, Boolean>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<Long, Boolean> validationResults = response.getBody();
                return validationResults.values().stream().allMatch(valid -> valid);
            }
            return false;
        } catch (Exception e) {
            logger.error("Error validating products", e);
            throw new InfrastructureServiceException(
                "Failed to validate products",
                "INVENTORY",
                "VALIDATE_PRODUCTS",
                e
            );
        }
    }

    @Override
    public void confirmReservation(Long orderId, List<DomainEntityOrderItem> items) {
        try {
            List<ReservationItem> reservationItems = items.stream()
                .map(item -> ReservationItem.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build())
                .collect(Collectors.toList());

            ConfirmationRequest request = ConfirmationRequest.builder()
                .orderId(orderId.toString())
                .items(reservationItems)
                .build();

            ResponseEntity<ConfirmationResponse> response = restTemplate.postForEntity(
                    inventoryServiceUrl + "/api/inventory/confirm-reservation",
                    request,
                    ConfirmationResponse.class);

            if (response.getStatusCode() != HttpStatus.OK || 
                response.getBody() == null || 
                !response.getBody().isSuccess()) {
                throw new InfrastructureServiceException(
                    "Failed to confirm reservation",
                    "INVENTORY",
                    "CONFIRM_RESERVATION"
                );
            }
        } catch (Exception e) {
            logger.error("Error confirming reservation", e);
            throw new InfrastructureServiceException(
                "Failed to confirm reservation",
                "INVENTORY",
                "CONFIRM_RESERVATION",
                e
            );
        }
    }

    @Override
    public void cancelReservation(Long orderId, List<DomainEntityOrderItem> items) {
        try {
            List<ReservationItem> reservationItems = items.stream()
                .map(item -> ReservationItem.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build())
                .collect(Collectors.toList());

            CancellationRequest request = CancellationRequest.builder()
                .orderId(orderId.toString())
                .items(reservationItems)
                .build();

            ResponseEntity<CancellationResponse> response = restTemplate.postForEntity(
                    inventoryServiceUrl + "/api/inventory/cancel-reservation",
                    request,
                    CancellationResponse.class);

            if (response.getStatusCode() != HttpStatus.OK || 
                response.getBody() == null || 
                !response.getBody().isSuccess()) {
                throw new InfrastructureServiceException(
                    "Failed to cancel reservation",
                    "INVENTORY",
                    "CANCEL_RESERVATION"
                );
            }
        } catch (Exception e) {
            logger.error("Error cancelling reservation", e);
            throw new InfrastructureServiceException(
                "Failed to cancel reservation",
                "INVENTORY",
                "CANCEL_RESERVATION",
                e
            );
        }
    }
}