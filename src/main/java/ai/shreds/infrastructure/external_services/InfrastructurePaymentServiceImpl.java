package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.entities.DomainEntityPaymentDetails;
import ai.shreds.domain.ports.DomainOutputPortPaymentService;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceException;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

@Service
public class InfrastructurePaymentServiceImpl implements DomainOutputPortPaymentService {

    private static final Logger log = LoggerFactory.getLogger(InfrastructurePaymentServiceImpl.class);

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
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
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
                        "PROCESS_PAYMENT"
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
                    "PROCESS_PAYMENT",
                    e
            );
        }
    }

    @Override
    public boolean validatePaymentDetails(DomainEntityPaymentDetails paymentDetails) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<PaymentRequest> entity = new HttpEntity<>(createPaymentRequest(paymentDetails), headers);
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    paymentServiceUrl + "/validate",
                    HttpMethod.POST,
                    entity,
                    Boolean.class
            );
            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            log.error("Error validating payment details", e);
            throw new InfrastructureServiceException("Payment validation failed", "PAYMENT", "VALIDATE_PAYMENT", e);
        }
    }

    @Override
    public Optional<DomainEntityPaymentDetails> getPaymentStatus(String paymentId) {
        try {
            ResponseEntity<PaymentResponse> response = restTemplate.getForEntity(
                    paymentServiceUrl + "/status/" + paymentId,
                    PaymentResponse.class
            );
            if (response.getBody() != null) {
                DomainEntityPaymentDetails details = new DomainEntityPaymentDetails();
                details.setPaymentId(response.getBody().getPaymentId());
                details.updateStatus(mapPaymentStatus(response.getBody().getStatus()));
                return Optional.of(details);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting payment status", e);
            throw new InfrastructureServiceException("Failed to get payment status", "PAYMENT", "GET_STATUS", e);
        }
    }

    @Override
    public void cancelPayment(String paymentId) {
        try {
            restTemplate.delete(paymentServiceUrl + "/cancel/" + paymentId);
        } catch (Exception e) {
            log.error("Error cancelling payment", e);
            throw new InfrastructureServiceException("Failed to cancel payment", "PAYMENT", "CANCEL_PAYMENT", e);
        }
    }

    @Override
    public DomainEntityPaymentDetails refundPayment(String paymentId, DomainValueMoney amount) {
        try {
            HttpHeaders headers = createHeaders();
            RefundRequest refundRequest = new RefundRequest(paymentId, amount.getAmount());
            HttpEntity<RefundRequest> entity = new HttpEntity<>(refundRequest, headers);
            
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    paymentServiceUrl + "/refund",
                    HttpMethod.POST,
                    entity,
                    PaymentResponse.class
            );

            if (response.getBody() != null) {
                DomainEntityPaymentDetails details = new DomainEntityPaymentDetails();
                details.setPaymentId(response.getBody().getPaymentId());
                details.updateStatus(mapPaymentStatus(response.getBody().getStatus()));
                return details;
            }
            throw new InfrastructureServiceException("Invalid refund response", "PAYMENT", "REFUND_PAYMENT");
        } catch (Exception e) {
            log.error("Error processing refund", e);
            throw new InfrastructureServiceException("Failed to process refund", "PAYMENT", "REFUND_PAYMENT", e);
        }
    }

    @Override
    public boolean isPaymentMethodSupported(String paymentMethod) {
        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(
                    paymentServiceUrl + "/methods/" + paymentMethod + "/supported",
                    Boolean.class
            );
            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            log.error("Error checking payment method support", e);
            throw new InfrastructureServiceException("Failed to check payment method", "PAYMENT", "CHECK_METHOD", e);
        }
    }

    @Override
    public boolean authorizePayment(DomainEntityPaymentDetails paymentDetails) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<PaymentRequest> entity = new HttpEntity<>(createPaymentRequest(paymentDetails), headers);
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    paymentServiceUrl + "/authorize",
                    HttpMethod.POST,
                    entity,
                    Boolean.class
            );
            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            log.error("Error authorizing payment", e);
            throw new InfrastructureServiceException("Failed to authorize payment", "PAYMENT", "AUTHORIZE_PAYMENT", e);
        }
    }

    @Override
    public DomainEntityPaymentDetails capturePayment(String paymentId) {
        try {
            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                    paymentServiceUrl + "/capture/" + paymentId,
                    null,
                    PaymentResponse.class
            );

            if (response.getBody() != null) {
                DomainEntityPaymentDetails details = new DomainEntityPaymentDetails();
                details.setPaymentId(response.getBody().getPaymentId());
                details.updateStatus(mapPaymentStatus(response.getBody().getStatus()));
                return details;
            }
            throw new InfrastructureServiceException("Invalid capture response", "PAYMENT", "CAPTURE_PAYMENT");
        } catch (Exception e) {
            log.error("Error capturing payment", e);
            throw new InfrastructureServiceException("Failed to capture payment", "PAYMENT", "CAPTURE_PAYMENT", e);
        }
    }

    private void handlePaymentError(HttpStatusCodeException e, DomainEntityPaymentDetails paymentDetails, boolean isServerError) {
        String errorMessage = String.format(
                "Payment service %s error: %s",
                isServerError ? "server" : "client",
                e.getResponseBodyAsString()
        );
        log.error(errorMessage);

        paymentDetails.updateStatus(SharedPaymentStatusEnum.FAILED);
        throw new InfrastructureServiceException(errorMessage, "PAYMENT", "PROCESS_PAYMENT", e);
    }

    private PaymentRequest createPaymentRequest(DomainEntityPaymentDetails paymentDetails) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(paymentDetails.getOrderId());
        request.setAmount(paymentDetails.getAmount().getAmount());
        request.setCurrency(paymentDetails.getAmount().getCurrency());
        request.setPaymentMethod(paymentDetails.getPaymentMethod());
        return request;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private DomainEntityPaymentDetails updatePaymentDetails(DomainEntityPaymentDetails paymentDetails, PaymentResponse response) {
        if (response != null) {
            String responseStatus = response.getStatus();
            if (responseStatus != null) {
                paymentDetails.setPaymentId(response.getPaymentId());
                paymentDetails.updateStatus(mapPaymentStatus(responseStatus));
                log.info("Payment processed successfully. Payment ID: {}, Status: {}",
                        response.getPaymentId(),
                        responseStatus);
            } else {
                log.warn("Payment response status is null, setting status to FAILED");
                paymentDetails.updateStatus(SharedPaymentStatusEnum.FAILED);
            }
        } else {
            log.warn("Payment response is null, setting status to FAILED");
            paymentDetails.updateStatus(SharedPaymentStatusEnum.FAILED);
        }
        return paymentDetails;
    }

    private SharedPaymentStatusEnum mapPaymentStatus(String status) {
        try {
            return SharedPaymentStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown payment status: {}, defaulting to FAILED", status);
            return SharedPaymentStatusEnum.FAILED;
        }
    }

    public static class PaymentRequest {
        private Long orderId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;

        public PaymentRequest() {}

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class PaymentResponse {
        private String paymentId;
        private String status;
        private String message;

        public PaymentResponse() {
            this.status = SharedPaymentStatusEnum.PENDING.name();
        }

        public PaymentResponse(String paymentId, String status, String message) {
            this.paymentId = paymentId;
            this.status = status;
            this.message = message;
        }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class RefundRequest {
        private String paymentId;
        private BigDecimal amount;

        public RefundRequest() {}

        public RefundRequest(String paymentId, BigDecimal amount) {
            this.paymentId = paymentId;
            this.amount = amount;
        }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
