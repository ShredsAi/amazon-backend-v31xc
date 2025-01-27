package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.ports.DomainOutputPortInventoryService;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Service
public class InfrastructureInventoryServiceImpl implements DomainOutputPortInventoryService {

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
            log.debug("Attempting to reserve {} items", items.size());
            String orderId = UUID.randomUUID().toString();

            ReservationRequest request = new ReservationRequest(orderId, 
                items.stream()
                    .map(item -> new ReservationItem(item.getProductId(), item.getQuantity()))
                    .collect(Collectors.toList()));

            ResponseEntity<ReservationResponse> response = restTemplate.postForEntity(
                    inventoryServiceUrl + "/api/inventory/reserve",
                    request,
                    ReservationResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ReservationResponse reservationResponse = response.getBody();
                if (reservationResponse.isSuccess()) {
                    log.info("Successfully reserved {} items with reservation ID: {}", 
                        items.size(), reservationResponse.getReservationId());
                    return true;
                } else {
                    log.warn("Failed to reserve items: {}", reservationResponse.getMessage());
                    return false;
                }
            }
            return false;

        } catch (Exception e) {
            log.error("Error during inventory reservation", e);
            throw new InfrastructureServiceException("Failed to reserve inventory", "INVENTORY", e);
        }
    }

    @Override
    @Retryable(value = {InfrastructureServiceException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void releaseItems(List<DomainEntityOrderItem> items) {
        try {
            log.debug("Attempting to release {} items", items.size());
            String orderId = UUID.randomUUID().toString();

            ReleaseRequest request = new ReleaseRequest(orderId,
                items.stream()
                    .map(item -> new ReservationItem(item.getProductId(), item.getQuantity()))
                    .collect(Collectors.toList()));

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
                    null
                );
            }

            log.info("Successfully released {} items", items.size());

        } catch (Exception e) {
            log.error("Error during inventory release", e);
            throw new InfrastructureServiceException("Failed to release inventory", "INVENTORY", e);
        }
    }

    // DTO classes for REST communication
    private record ReservationItem(Long productId, Integer quantity) {}
    
    private record ReservationRequest(String orderId, List<ReservationItem> items) {}
    
    private record ReservationResponse(boolean success, String reservationId, String message) {}
    
    private record ReleaseRequest(String orderId, List<ReservationItem> items) {}
    
    private record ReleaseResponse(boolean success, String message) {}
}
