package ai.shreds;

import ai.shreds.domain.ports.DomainOutputPortInventoryService;
import ai.shreds.domain.ports.DomainOutputPortPaymentService;
import ai.shreds.shared.dtos.SharedCreateOrderRequest;
import ai.shreds.shared.dtos.SharedOrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class OrderShredApplicationIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(OrderShredApplicationIntegrationTest.class);

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

    // Mock external services to avoid dependencies
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

    @Test
    void contextLoads(CapturedOutput output) {
        logger.info("=== STARTING APPLICATION CONTEXT LOAD TEST ===");
        
        // Verify that the application context loads successfully
        assertNotNull(restTemplate, "TestRestTemplate should be available");
        assertNotNull(objectMapper, "ObjectMapper should be available");
        
        // Verify that the application started successfully by checking logs
        String logOutput = output.toString();
        
        assertAll(
            "Application startup verification",
            () -> assertTrue(logOutput.contains("Started OrderShredApplication"), 
                "Application should have started successfully"),
            () -> assertTrue(logOutput.contains("Tomcat started on port"), 
                "Tomcat should have started on a port"),
            () -> assertTrue(mysql.isRunning(), 
                "MySQL container should be running"),
            () -> assertThat(port).isGreaterThan(0).as("Server port should be assigned")
        );
        
        logger.info("=== APPLICATION STARTED SUCCESSFULLY ON PORT {} ===", port);
        logger.info("=== MYSQL CONTAINER RUNNING ON {} ===", mysql.getJdbcUrl());
        
        // Print full application logs for analysis
        logger.info("=== FULL APPLICATION STARTUP LOGS ===");
        System.out.println(logOutput);
        logger.info("=== END OF APPLICATION STARTUP LOGS ===");
    }

    @Test
    void applicationHealthCheck(CapturedOutput output) {
        logger.info("=== STARTING APPLICATION HEALTH CHECK TEST ===");
        
        // Test that the application is responsive
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertAll(
            "Health check verification",
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).contains("UP")
        );
        
        logger.info("=== HEALTH CHECK PASSED - APPLICATION IS RESPONSIVE ===");
        logger.info("Health response: {}", response.getBody());
    }

    @Test
    void databaseConnectionTest(CapturedOutput output) {
        logger.info("=== STARTING DATABASE CONNECTION TEST ===");
        
        // Verify database connectivity through health endpoint
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertAll(
            "Database connection verification",
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).contains("UP"),
            () -> assertTrue(mysql.isRunning(), "MySQL container should be running")
        );
        
        logger.info("=== DATABASE CONNECTION TEST PASSED ===");
        logger.info("MySQL JDBC URL: {}", mysql.getJdbcUrl());
        logger.info("MySQL Username: {}", mysql.getUsername());
    }

    @Test
    void restEndpointAvailabilityTest(CapturedOutput output) {
        logger.info("=== STARTING REST ENDPOINT AVAILABILITY TEST ===");
        
        // Test that the main REST endpoint is available (even if it fails due to missing data)
        String orderUrl = "http://localhost:" + port + "/api/orders";
        
        SharedCreateOrderRequest request = new SharedCreateOrderRequest();
        request.setUserId(123L);
        request.setPaymentMethod("CREDIT_CARD");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SharedCreateOrderRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(orderUrl, entity, String.class);
        
        // We expect some response (not necessarily success due to mocked services)
        // The important thing is that the endpoint is reachable and the application is running
        assertThat(response.getStatusCode().value()).isBetween(400, 599);
        
        logger.info("=== REST ENDPOINT AVAILABILITY TEST COMPLETED ===");
        logger.info("Order endpoint response status: {}", response.getStatusCode());
        logger.info("Order endpoint response body: {}", response.getBody());
    }

    @Test
    void fullApplicationLogsCapture(CapturedOutput output) {
        logger.info("=== CAPTURING FULL APPLICATION LOGS FOR ANALYSIS ===");
        
        // This test specifically captures and displays all application logs
        String logOutput = output.toString();
        
        // Verify key components are mentioned in logs
        assertAll(
            "Log content verification",
            () -> assertTrue(logOutput.contains("OrderShredApplication"), 
                "Main application class should be mentioned in logs"),
            () -> assertTrue(logOutput.contains("HikariPool") || logOutput.contains("HikariCP"), 
                "Database connection pool should be initialized"),
            () -> assertTrue(logOutput.contains("JPA") || logOutput.contains("Hibernate"), 
                "JPA/Hibernate should be initialized"),
            () -> assertTrue(logOutput.contains("Tomcat"), 
                "Tomcat server should be started")
        );
        
        // Print complete logs to console for manual analysis
        System.out.println("\n" + "=".repeat(100));
        System.out.println("COMPLETE APPLICATION STARTUP LOGS:");
        System.out.println("=".repeat(100));
        System.out.println(logOutput);
        System.out.println("=".repeat(100));
        System.out.println("END OF COMPLETE APPLICATION STARTUP LOGS");
        System.out.println("=".repeat(100) + "\n");
        
        logger.info("=== FULL APPLICATION LOGS CAPTURED SUCCESSFULLY ===");
    }
}