package ai.shreds;

import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.entities.DomainEntityPaymentDetails;
import ai.shreds.domain.exceptions.DomainExceptionPaymentFailed;
import ai.shreds.domain.ports.DomainOutputPortInventoryService;
import ai.shreds.domain.ports.DomainOutputPortPaymentService;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.infrastructure.repositories.SpringDataOrderRepository;
import ai.shreds.infrastructure.repositories.SpringDataOrderItemRepository;
import ai.shreds.infrastructure.repositories.SpringDataPaymentDetailsRepository;
import ai.shreds.shared.dtos.SharedCreateOrderRequest;
import ai.shreds.shared.dtos.SharedOrderResponse;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Transactional
class OrderCreationIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreationIntegrationTest.class);

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_orders")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataOrderRepository orderRepository;

    @Autowired
    private SpringDataOrderItemRepository orderItemRepository;

    @Autowired
    private SpringDataPaymentDetailsRepository paymentDetailsRepository;

    // Mock external services
    @MockBean
    private DomainOutputPortInventoryService inventoryService;

    @MockBean
    private DomainOutputPortPaymentService paymentService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        
        // Disable ActiveMQ for this test
        registry.add("spring.activemq.broker-url", () -> "vm://localhost?broker.persistent=false");
        
        // Mock external service URLs
        registry.add("payment.service.url", () -> "http://localhost:8081");
        registry.add("inventory.service.host", () -> "localhost");
        registry.add("inventory.service.port", () -> "9090");
    }

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        paymentDetailsRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void When_Valid_Order_Request_Then_Order_Created_Successfully(CapturedOutput output) {
        logger.info("=== STARTING ORDER CREATION SUCCESS TEST ===");
        
        // Given: Mock successful inventory reservation
        when(inventoryService.reserveItems(anyList())).thenReturn(true);
        
        // Given: Mock successful payment processing
        DomainEntityPaymentDetails successfulPayment = createSuccessfulPaymentDetails();
        when(paymentService.processPayment(any(DomainEntityPaymentDetails.class)))
                .thenReturn(successfulPayment);
        
        // Given: Valid order request
        SharedCreateOrderRequest request = new SharedCreateOrderRequest();
        request.setUserId(123L);
        request.setPaymentMethod("CREDIT_CARD");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SharedCreateOrderRequest> entity = new HttpEntity<>(request, headers);
        
        String orderUrl = "http://localhost:" + port + "/api/orders";
        
        logger.info("Sending order creation request to: {}", orderUrl);
        logger.info("Request payload: {}", request);
        
        // When: Create order
        ResponseEntity<SharedOrderResponse> response = restTemplate.postForEntity(
                orderUrl, entity, SharedOrderResponse.class);
        
        logger.info("Received response status: {}", response.getStatusCode());
        logger.info("Response body: {}", response.getBody());
        
        // Then: Verify response
        assertAll(
            "Order creation response verification",
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
            () -> assertNotNull(response.getBody(), "Response body should not be null"),
            () -> assertThat(response.getBody().getId()).isNotNull().isPositive(),
            () -> assertThat(response.getBody().getUserId()).isEqualTo(123L),
            () -> assertThat(response.getBody().getPaymentStatus()).isEqualTo("SUCCESS"),
            () -> assertThat(response.getBody().getReservationState()).isEqualTo("COMPLETED"),
            () -> assertThat(response.getBody().getTotalAmount()).isNotNull().isPositive(),
            () -> assertThat(response.getBody().getCreatedAt()).isNotNull(),
            () -> assertThat(response.getBody().getItems()).isNotNull()
        );
        
        // Then: Verify database persistence
        Long orderId = response.getBody().getId();
        assertThat(orderRepository.findById(orderId)).isPresent();
        
        // Then: Verify external service interactions
        verify(inventoryService, times(1)).reserveItems(anyList());
        verify(paymentService, times(1)).processPayment(any(DomainEntityPaymentDetails.class));
        
        logger.info("=== ORDER CREATION SUCCESS TEST COMPLETED SUCCESSFULLY ===");
        
        // Print logs for analysis
        String logOutput = output.toString();
        System.out.println("\n" + "=".repeat(100));
        System.out.println("ORDER CREATION TEST LOGS:");
        System.out.println("=".repeat(100));
        System.out.println(logOutput);
        System.out.println("=".repeat(100) + "\n");
    }

    @Test
    void When_Payment_Fails_Then_Order_Creation_Fails_And_Inventory_Released(CapturedOutput output) {
        logger.info("=== STARTING PAYMENT FAILURE TEST ===");
        
        // Given: Mock successful inventory reservation
        when(inventoryService.reserveItems(anyList())).thenReturn(true);
        
        // Given: Mock successful inventory release (for rollback)
        doNothing().when(inventoryService).releaseItems(anyList());
        
        // Given: Mock payment failure
        DomainExceptionPaymentFailed paymentFailedException = DomainExceptionPaymentFailed.newBuilder()
                .message("Payment declined by issuer")
                .reason(DomainExceptionPaymentFailed.FailureReason.CARD_DECLINED)
                .paymentId("payment-failed-123")
                .paymentMethod("CREDIT_CARD")
                .amount(DomainValueMoney.of(BigDecimal.valueOf(99.99), "USD"))
                .build();
        
        when(paymentService.processPayment(any(DomainEntityPaymentDetails.class)))
                .thenThrow(paymentFailedException);
        
        // Given: Valid order request
        SharedCreateOrderRequest request = new SharedCreateOrderRequest();
        request.setUserId(456L);
        request.setPaymentMethod("CREDIT_CARD");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SharedCreateOrderRequest> entity = new HttpEntity<>(request, headers);
        
        String orderUrl = "http://localhost:" + port + "/api/orders";
        
        logger.info("Sending order creation request with payment failure scenario to: {}", orderUrl);
        logger.info("Request payload: {}", request);
        
        // When: Attempt to create order (should fail)
        ResponseEntity<String> response = restTemplate.postForEntity(
                orderUrl, entity, String.class);
        
        logger.info("Received response status: {}", response.getStatusCode());
        logger.info("Response body: {}", response.getBody());
        
        // Then: Verify failure response
        assertAll(
            "Payment failure response verification",
            () -> assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody()).containsIgnoringCase("payment")
        );
        
        // Then: Verify no order was persisted in database
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(orderItemRepository.findAll()).isEmpty();
        assertThat(paymentDetailsRepository.findAll()).isEmpty();
        
        // Then: Verify external service interactions
        verify(inventoryService, times(1)).reserveItems(anyList());
        verify(inventoryService, times(1)).releaseItems(anyList()); // Inventory should be released on payment failure
        verify(paymentService, times(1)).processPayment(any(DomainEntityPaymentDetails.class));
        
        logger.info("=== PAYMENT FAILURE TEST COMPLETED SUCCESSFULLY ===");
        
        // Print logs for analysis
        String logOutput = output.toString();
        System.out.println("\n" + "=".repeat(100));
        System.out.println("PAYMENT FAILURE TEST LOGS:");
        System.out.println("=".repeat(100));
        System.out.println(logOutput);
        System.out.println("=".repeat(100) + "\n");
    }
    
    private DomainEntityPaymentDetails createSuccessfulPaymentDetails() {
        DomainEntityPaymentDetails paymentDetails = new DomainEntityPaymentDetails();
        paymentDetails.setPaymentId("payment-123");
        paymentDetails.setOrderId(1L);
        paymentDetails.setPaymentMethod("CREDIT_CARD");
        paymentDetails.setAmount(DomainValueMoney.of(BigDecimal.valueOf(99.99), "USD"));
        paymentDetails.setStatus(DomainValuePaymentStatus.of(SharedPaymentStatusEnum.SUCCESS));
        return paymentDetails;
    }
    
    private List<DomainEntityOrderItem> createSampleOrderItems() {
        DomainEntityOrderItem item1 = new DomainEntityOrderItem();
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setPrice(DomainValueMoney.of(BigDecimal.valueOf(29.99), "USD"));
        
        DomainEntityOrderItem item2 = new DomainEntityOrderItem();
        item2.setProductId(2L);
        item2.setQuantity(1);
        item2.setPrice(DomainValueMoney.of(BigDecimal.valueOf(39.99), "USD"));
        
        return Arrays.asList(item1, item2);
    }
}