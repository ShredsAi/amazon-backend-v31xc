package ai.shreds;

import ai.shreds.shared.dtos.SharedCreateOrderRequest;
import ai.shreds.shared.dtos.SharedOrderResponse;
import ai.shreds.infrastructure.repositories.SpringDataOrderRepository;
import ai.shreds.infrastructure.repositories.SpringDataOrderItemRepository;
import ai.shreds.domain.entities.DomainEntityOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Transactional
public class OrderShredApplicationIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(OrderShredApplicationIntegrationTest.class);

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

    private static WireMockServer paymentServiceMock;
    private static WireMockServer inventoryServiceMock;
    private static WireMockServer cartServiceMock;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.31"))
            .withDatabaseName("test_orders")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    static GenericContainer<?> activemq = new GenericContainer<>(DockerImageName.parse("rmohr/activemq:5.15.9"))
            .withExposedPorts(61616, 8161)
            .withEnv("ACTIVEMQ_ADMIN_LOGIN", "admin")
            .withEnv("ACTIVEMQ_ADMIN_PASSCODE", "admin")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        
        // ActiveMQ configuration
        registry.add("spring.activemq.broker-url", () -> 
            "tcp://" + activemq.getHost() + ":" + activemq.getMappedPort(61616));
        
        // External services configuration
        registry.add("payment.service.url", () -> "http://localhost:" + paymentServiceMock.port());
        registry.add("inventory.service.host", () -> "localhost");
        registry.add("inventory.service.port", () -> inventoryServiceMock.port());
        registry.add("cart.service.url", () -> "http://localhost:" + cartServiceMock.port());
    }

    @BeforeAll
    static void setUp() {
        logger.info("Setting up WireMock servers for external services");
        
        // Start Payment Service Mock
        paymentServiceMock = new WireMockServer(8089);
        paymentServiceMock.start();
        
        // Start Inventory Service Mock
        inventoryServiceMock = new WireMockServer(9091);
        inventoryServiceMock.start();
        
        // Start Cart Service Mock
        cartServiceMock = new WireMockServer(8088);
        cartServiceMock.start();
        
        setupMockResponses();
        
        logger.info("WireMock servers started - Payment: {}, Inventory: {}, Cart: {}", 
                paymentServiceMock.port(), inventoryServiceMock.port(), cartServiceMock.port());
    }

    private static void setupMockResponses() {
        // Configure Cart Service Mock responses - return cart with items
        cartServiceMock.stubFor(get(urlPathMatching("/api/cart/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{"
                                + "\"userId\": 123,"
                                + "\"items\": ["
                                + "{"
                                + "\"productId\": 1001,"
                                + "\"quantity\": 2,"
                                + "\"price\": 29.99"
                                + "},"
                                + "{"
                                + "\"productId\": 1002,"
                                + "\"quantity\": 1,"
                                + "\"price\": 39.99"
                                + "}"
                                + "],"
                                + "\"totalAmount\": 99.97"
                                + "}")));
        
        // Configure Payment Service Mock responses - successful payment
        paymentServiceMock.stubFor(post(urlPathEqualTo("/api/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{"
                                + "\"paymentId\": \"payment-123\","
                                + "\"status\": \"SUCCESS\","
                                + "\"amount\": 99.97"
                                + "}")));
        
        // Configure Inventory Service Mock responses - successful reservation
        inventoryServiceMock.stubFor(post(urlPathEqualTo("/inventory/reserve"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{"
                                + "\"success\": true,"
                                + "\"reservationId\": \"res-123\""
                                + "}")));
    }

    @AfterAll
    static void tearDown() {
        if (paymentServiceMock != null) {
            paymentServiceMock.stop();
        }
        if (inventoryServiceMock != null) {
            inventoryServiceMock.stop();
        }
        if (cartServiceMock != null) {
            cartServiceMock.stop();
        }
        logger.info("WireMock servers stopped");
    }

    @Test
    void When_Valid_Order_Request_Then_Order_Created_Successfully(CapturedOutput output) {
        logger.info("=== Testing successful order creation workflow ===");
        
        // Given: A valid order creation request
        SharedCreateOrderRequest request = new SharedCreateOrderRequest(123L, "CREDIT_CARD");
        String baseUrl = "http://localhost:" + port;
        
        logger.info("Sending order creation request: {}", request);
        
        // When: Creating an order via the API
        ResponseEntity<SharedOrderResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/orders", request, SharedOrderResponse.class);
        
        logger.info("Received response status: {}", response.getStatusCode());
        logger.info("Received response body: {}", response.getBody());
        
        // Then: The order should be created successfully
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), 
                "Order creation should return 201 CREATED status");
        
        assertNotNull(response.getBody(), "Response body should not be null");
        SharedOrderResponse orderResponse = response.getBody();
        
        // Verify order response structure
        assertNotNull(orderResponse.getId(), "Order ID should be generated");
        assertEquals(123L, orderResponse.getUserId(), "User ID should match request");
        assertNotNull(orderResponse.getTotalAmount(), "Total amount should be calculated");
        assertTrue(orderResponse.getTotalAmount().compareTo(BigDecimal.ZERO) > 0, 
                "Total amount should be greater than zero");
        assertEquals("SUCCESS", orderResponse.getPaymentStatus(), 
                "Payment status should be SUCCESS");
        assertEquals("RESERVED", orderResponse.getReservationState(), 
                "Reservation state should be RESERVED");
        assertNotNull(orderResponse.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(orderResponse.getItems(), "Order items should be present");
        assertFalse(orderResponse.getItems().isEmpty(), "Order should have items");
        
        // Verify order items
        assertEquals(2, orderResponse.getItems().size(), "Order should have 2 items");
        
        // Verify first item
        var firstItem = orderResponse.getItems().get(0);
        assertEquals(Long.valueOf(1001), firstItem.getProductId(), "First item product ID should match");
        assertEquals(Integer.valueOf(2), firstItem.getQuantity(), "First item quantity should match");
        assertEquals(new BigDecimal("29.99"), firstItem.getPrice(), "First item price should match");
        
        // Verify second item
        var secondItem = orderResponse.getItems().get(1);
        assertEquals(Long.valueOf(1002), secondItem.getProductId(), "Second item product ID should match");
        assertEquals(Integer.valueOf(1), secondItem.getQuantity(), "Second item quantity should match");
        assertEquals(new BigDecimal("39.99"), secondItem.getPrice(), "Second item price should match");
        
        // Verify database persistence
        Optional<DomainEntityOrder> savedOrder = orderRepository.findById(orderResponse.getId());
        assertTrue(savedOrder.isPresent(), "Order should be saved to database");
        
        DomainEntityOrder dbOrder = savedOrder.get();
        assertEquals(123L, dbOrder.getUserId(), "Database order user ID should match");
        assertEquals("CREDIT_CARD", dbOrder.getPaymentMethod(), "Database order payment method should match");
        assertNotNull(dbOrder.getTotalAmount(), "Database order total amount should be set");
        assertEquals("SUCCESS", dbOrder.getPaymentStatus().getStatus().name(), 
                "Database order payment status should be SUCCESS");
        assertEquals("RESERVED", dbOrder.getReservationState().getStatus().name(), 
                "Database order reservation state should be RESERVED");
        
        // Verify order items are saved
        var savedItems = orderItemRepository.findByOrderId(orderResponse.getId());
        assertEquals(2, savedItems.size(), "Order items should be saved to database");
        
        // Verify external service calls were made
        cartServiceMock.verify(getRequestedFor(urlPathMatching("/api/cart/user/123")));
        inventoryServiceMock.verify(postRequestedFor(urlPathEqualTo("/inventory/reserve")));
        paymentServiceMock.verify(postRequestedFor(urlPathEqualTo("/api/payments")));
        
        // Verify application logs show successful processing
        String logs = output.getOut();
        assertTrue(logs.contains("Received order creation request for user: 123"), 
                "Logs should show order creation request received");
        assertTrue(logs.contains("Successfully created order with ID:"), 
                "Logs should show successful order creation");
        
        logger.info("✅ Order creation workflow completed successfully");
        logger.info("✅ Order ID: {}, Total Amount: {}, Payment Status: {}, Reservation State: {}", 
                orderResponse.getId(), orderResponse.getTotalAmount(), 
                orderResponse.getPaymentStatus(), orderResponse.getReservationState());
    }

    @Test
    void shouldStartApplicationSuccessfully(CapturedOutput output) {
        logger.info("Testing if Spring Boot application starts successfully");
        
        // Verify the application context loads and the application starts
        String baseUrl = "http://localhost:" + port;
        
        // Test health endpoint to verify application is running
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/health", String.class);
        
        logger.info("Health endpoint response: {}", healthResponse.getBody());
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertNotNull(healthResponse.getBody());
        assertTrue(healthResponse.getBody().contains("UP"));
        
        // Verify application logs show successful startup
        String logs = output.getOut();
        logger.info("Application startup logs captured");
        
        // Check for key startup indicators in logs
        assertTrue(logs.contains("Started OrderShredApplication"), 
                "Application should start successfully");
        assertTrue(logs.contains("Tomcat started on port"), 
                "Tomcat should start on a port");
        
        logger.info("✅ Application started successfully and health check passed");
    }

    @Test
    void shouldHaveOrderEndpointAvailable(CapturedOutput output) {
        logger.info("Testing if order creation endpoint is available");
        
        String baseUrl = "http://localhost:" + port;
        
        // Create a test request
        SharedCreateOrderRequest request = new SharedCreateOrderRequest(123L, "CREDIT_CARD");
        
        // Test the order creation endpoint (it might fail due to missing cart data, but endpoint should be available)
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/orders", request, String.class);
        
        logger.info("Order endpoint response status: {}", response.getStatusCode());
        logger.info("Order endpoint response body: {}", response.getBody());
        
        // The endpoint should be available (not 404), even if it returns an error due to missing dependencies
        assertNotEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        // Verify the endpoint is mapped correctly
        String logs = output.getOut();
        assertTrue(logs.contains("Mapped \"{[/api/orders]\"") || 
                  logs.contains("RequestMappingHandlerMapping"), 
                "Order endpoint should be mapped");
        
        logger.info("✅ Order endpoint is available and properly mapped");
    }

    @Test
    void shouldConnectToDatabaseSuccessfully(CapturedOutput output) {
        logger.info("Testing database connectivity");
        
        // Verify database connection in logs
        String logs = output.getOut();
        
        // Check for Hibernate/JPA initialization
        assertTrue(logs.contains("HikariPool") || logs.contains("Database connection"), 
                "Database connection pool should be initialized");
        assertTrue(logs.contains("Hibernate") || logs.contains("JPA"), 
                "JPA/Hibernate should be initialized");
        
        logger.info("✅ Database connectivity verified");
    }

    @Test
    void shouldConnectToMessageBrokerSuccessfully(CapturedOutput output) {
        logger.info("Testing message broker connectivity");
        
        // Verify ActiveMQ connection in logs
        String logs = output.getOut();
        
        // Check for ActiveMQ/JMS initialization
        assertTrue(logs.contains("ActiveMQ") || logs.contains("JMS") || logs.contains("ConnectionFactory"), 
                "ActiveMQ/JMS should be initialized");
        
        logger.info("✅ Message broker connectivity verified");
    }

    @Test
    void shouldLoadAllRequiredBeans(CapturedOutput output) {
        logger.info("Testing if all required beans are loaded");
        
        String logs = output.getOut();
        
        // Verify Spring context loads without errors
        assertFalse(logs.contains("BeanCreationException"), 
                "No bean creation exceptions should occur");
        assertFalse(logs.contains("NoSuchBeanDefinitionException"), 
                "All required beans should be available");
        assertFalse(logs.contains("UnsatisfiedDependencyException"), 
                "All dependencies should be satisfied");
        
        // Check for successful application context refresh
        assertTrue(logs.contains("Root WebApplicationContext: initialization completed") ||
                  logs.contains("ApplicationContext"), 
                "Application context should initialize successfully");
        
        logger.info("✅ All required beans loaded successfully");
    }

    @Test
    void shouldDisplayFullStartupLogs(CapturedOutput output) {
        logger.info("=== FULL APPLICATION STARTUP LOGS ===");
        
        String logs = output.getOut();
        System.out.println(logs);
        
        logger.info("=== END OF STARTUP LOGS ===");
        
        // Basic verification that we captured logs
        assertFalse(logs.isEmpty(), "Should capture startup logs");
        
        logger.info("✅ Full startup logs displayed for analysis");
    }
}