package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.entities.DomainEntityPaymentDetails;
import ai.shreds.domain.ports.DomainOutputPortPaymentService;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceException;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Service
public class InfrastructurePaymentServiceImpl implements DomainOutputPortPaymentService {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public InfrastructurePaymentServiceImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${payment.service.url}") String paymentServiceUrl,
            @Value("${payment.service.timeout:5000}") int timeout
    ) {
        this.paymentServiceUrl = paymentServiceUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .build();
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public DomainEntityPaymentDetails processPayment(DomainEntityPaymentDetails paymentDetails) {
        try {
            log.debug("Processing payment for order ID: {}, amount: {}", 
                    paymentDetails.getOrderId(), 
                    paymentDetails.getAmount().getAmount());

            PaymentRequest request = createPaymentRequest(paymentDetails);
            HttpHeaders headers = createHeaders();
            HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    paymentServiceUrl + "/process",
                    HttpMethod.POST,
                    entity,
                    PaymentResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return updatePaymentDetails(paymentDetails, response.getBody());
            } else {
                throw new InfrastructureServiceException(
                        "Invalid response from payment service",
                        "PAYMENT",
                        null
                );
            }

        } catch (HttpClientErrorException e) {
            log.error("Client error during payment processing: {}", e.getMessage());
            handlePaymentError(e, paymentDetails, false);
            return paymentDetails;

        } catch (HttpServerErrorException e) {
            log.error("Server error during payment processing: {}", e.getMessage());
            handlePaymentError(e, paymentDetails, true);
            return paymentDetails;

        } catch (Exception e) {
            log.error("Unexpected error during payment processing", e);
            throw new InfrastructureServiceException(
                    "Payment processing failed",
                    "PAYMENT",
                    e
            );
        }
    }

    private DomainEntityPaymentDetails processPaymentFallback(DomainEntityPaymentDetails paymentDetails, Exception e) {
        log.error("Payment service fallback triggered due to: {}", e.getMessage());
        paymentDetails.setStatus(new DomainValuePaymentStatus(SharedPaymentStatusEnum.FAILED));
        return paymentDetails;
    }

    private PaymentRequest createPaymentRequest(DomainEntityPaymentDetails paymentDetails) {
        return PaymentRequest.builder()
                .orderId(paymentDetails.getOrderId())
                .amount(paymentDetails.getAmount().getAmount())
                .currency(paymentDetails.getAmount().getCurrency())
                .paymentMethod(paymentDetails.getPaymentMethod())
                .build();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private DomainEntityPaymentDetails updatePaymentDetails(DomainEntityPaymentDetails paymentDetails, PaymentResponse response) {
        paymentDetails.setPaymentId(response.getPaymentId());
        paymentDetails.setStatus(new DomainValuePaymentStatus(mapPaymentStatus(response.getStatus())));
        log.info("Payment processed successfully. Payment ID: {}, Status: {}", 
                response.getPaymentId(), 
                response.getStatus());
        return paymentDetails;
    }

    private void handlePaymentError(HttpStatusCodeException e, DomainEntityPaymentDetails paymentDetails, boolean isServerError) {
        String errorMessage = String.format(
                "Payment service %s error: %s",
                isServerError ? "server" : "client",
                e.getResponseBodyAsString()
        );
        log.error(errorMessage);

        paymentDetails.setStatus(new DomainValuePaymentStatus(SharedPaymentStatusEnum.FAILED));
        throw new InfrastructureServiceException(errorMessage, "PAYMENT", e);
    }

    private SharedPaymentStatusEnum mapPaymentStatus(String status) {
        try {
            return SharedPaymentStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown payment status: {}, defaulting to FAILED", status);
            return SharedPaymentStatusEnum.FAILED;
        }
    }

    @lombok.Value
    @lombok.Builder
    private static class PaymentRequest {
        Long orderId;
        java.math.BigDecimal amount;
        String currency;
        String paymentMethod;
    }

    @lombok.Value
    private static class PaymentResponse {
        String paymentId;
        String status;
        String message;
    }
}
