package ai.shreds;

import ai.shreds.domain.entities.DomainEntityOrder;
import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.entities.DomainEntityPaymentDetails;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValueOrderStatus;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.infrastructure.repositories.SpringDataOrderRepository;
import ai.shreds.infrastructure.repositories.SpringDataOrderItemRepository;
import ai.shreds.infrastructure.repositories.SpringDataPaymentDetailsRepository;
import ai.shreds.infrastructure.repositories.InfrastructureOrderRepositoryImpl;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Transactional
class OrderRepositoryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(OrderRepositoryIntegrationTest.class);

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_orders")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Autowired
    private SpringDataOrderRepository orderRepository;

    @Autowired
    private SpringDataOrderItemRepository orderItemRepository;

    @Autowired
    private SpringDataPaymentDetailsRepository paymentDetailsRepository;

    @Autowired
    private InfrastructureOrderRepositoryImpl orderRepositoryImpl;

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
        
        logger.info("=== DATABASE CLEANED UP FOR TEST ====");
    }

    @Test
    void When_Order_Saved_Then_Order_And_Items_Persisted_Correctly(CapturedOutput output) {
        logger.info("=== STARTING ORDER REPOSITORY INTEGRATION TEST ====");
        
        // Given: Create a complete order with items and payment details
        DomainEntityOrder order = createTestOrder();
        DomainEntityOrderItem item1 = createTestOrderItem(1L, 2, new BigDecimal("29.99"));
        DomainEntityOrderItem item2 = createTestOrderItem(2L, 1, new BigDecimal("39.99"));
        
        order.addItem(item1);
        order.addItem(item2);
        
        // Calculate total amount
        BigDecimal totalAmount = item1.getPrice().getAmount().multiply(new BigDecimal(item1.getQuantity()))
                .add(item2.getPrice().getAmount().multiply(new BigDecimal(item2.getQuantity())));
        order.setTotalAmount(DomainValueMoney.of(totalAmount, "USD"));
        
        logger.info("Created test order: {}", order);
        logger.info("Order items count: {}", order.getItems().size());
        logger.info("Total amount: {}", order.getTotalAmount());
        
        // When: Save the order using repository implementation
        DomainEntityOrder savedOrder = orderRepositoryImpl.save(order);
        
        logger.info("Saved order with ID: {}", savedOrder.getId());
        
        // Set order ID for items and save them
        item1.setOrderId(savedOrder.getId());
        item2.setOrderId(savedOrder.getId());
        orderRepositoryImpl.saveItems(List.of(item1, item2));
        
        logger.info("Saved order items for order ID: {}", savedOrder.getId());
        
        // Create and save payment details
        DomainEntityPaymentDetails paymentDetails = createTestPaymentDetails(savedOrder.getId());
        DomainEntityPaymentDetails savedPaymentDetails = paymentDetailsRepository.save(paymentDetails);
        
        logger.info("Saved payment details: {}", savedPaymentDetails);
        
        // Then: Verify order persistence
        assertAll(
            "Order persistence verification",
            () -> {
                assertNotNull(savedOrder.getId(), "Order ID should be generated");
                assertThat(savedOrder.getId()).isPositive();
                logger.info("✓ Order ID generated: {}", savedOrder.getId());
            },
            () -> {
                assertThat(savedOrder.getUserId()).isEqualTo(123L);
                logger.info("✓ User ID persisted correctly: {}", savedOrder.getUserId());
            },
            () -> {
                assertThat(savedOrder.getTotalAmount().getAmount()).isEqualByComparingTo(totalAmount);
                logger.info("✓ Total amount persisted correctly: {}", savedOrder.getTotalAmount());
            },
            () -> {
                assertThat(savedOrder.getPaymentStatus().getStatus()).isEqualTo(SharedPaymentStatusEnum.SUCCESS);
                logger.info("✓ Payment status persisted correctly: {}", savedOrder.getPaymentStatus().getStatus());
            },
            () -> {
                assertThat(savedOrder.getReservationState().getStatus()).isEqualTo(SharedOrderStatusEnum.COMPLETED);
                logger.info("✓ Reservation state persisted correctly: {}", savedOrder.getReservationState().getStatus());
            },
            () -> {
                assertNotNull(savedOrder.getCreatedAt());
                logger.info("✓ Created at timestamp persisted: {}", savedOrder.getCreatedAt());
            },
            () -> {
                assertThat(savedOrder.getPaymentMethod()).isEqualTo("CREDIT_CARD");
                logger.info("✓ Payment method persisted correctly: {}", savedOrder.getPaymentMethod());
            }
        );
        
        // Then: Verify order retrieval
        Optional<DomainEntityOrder> retrievedOrderOpt = orderRepositoryImpl.findById(savedOrder.getId());
        assertThat(retrievedOrderOpt).isPresent();
        
        DomainEntityOrder retrievedOrder = retrievedOrderOpt.get();
        logger.info("Retrieved order: {}", retrievedOrder);
        
        assertAll(
            "Order retrieval verification",
            () -> {
                assertThat(retrievedOrder.getId()).isEqualTo(savedOrder.getId());
                logger.info("✓ Retrieved order ID matches: {}", retrievedOrder.getId());
            },
            () -> {
                assertThat(retrievedOrder.getUserId()).isEqualTo(123L);
                logger.info("✓ Retrieved user ID matches: {}", retrievedOrder.getUserId());
            },
            () -> {
                assertThat(retrievedOrder.getTotalAmount().getAmount()).isEqualByComparingTo(totalAmount);
                logger.info("✓ Retrieved total amount matches: {}", retrievedOrder.getTotalAmount());
            }
        );
        
        // Then: Verify order items persistence and retrieval
        List<DomainEntityOrderItem> retrievedItems = orderRepositoryImpl.findItemsByOrderId(savedOrder.getId());
        assertThat(retrievedItems).hasSize(2);
        logger.info("Retrieved {} order items", retrievedItems.size());
        
        assertAll(
            "Order items verification",
            () -> {
                DomainEntityOrderItem retrievedItem1 = retrievedItems.stream()
                    .filter(item -> item.getProductId().equals(1L))
                    .findFirst().orElse(null);
                assertNotNull(retrievedItem1, "Item 1 should be found");
                assertThat(retrievedItem1.getQuantity()).isEqualTo(2);
                assertThat(retrievedItem1.getPrice().getAmount()).isEqualByComparingTo(new BigDecimal("29.99"));
                assertThat(retrievedItem1.getOrderId()).isEqualTo(savedOrder.getId());
                logger.info("✓ Item 1 persisted correctly: {}", retrievedItem1);
            },
            () -> {
                DomainEntityOrderItem retrievedItem2 = retrievedItems.stream()
                    .filter(item -> item.getProductId().equals(2L))
                    .findFirst().orElse(null);
                assertNotNull(retrievedItem2, "Item 2 should be found");
                assertThat(retrievedItem2.getQuantity()).isEqualTo(1);
                assertThat(retrievedItem2.getPrice().getAmount()).isEqualByComparingTo(new BigDecimal("39.99"));
                assertThat(retrievedItem2.getOrderId()).isEqualTo(savedOrder.getId());
                logger.info("✓ Item 2 persisted correctly: {}", retrievedItem2);
            }
        );
        
        // Then: Verify payment details persistence and retrieval
        Optional<DomainEntityPaymentDetails> retrievedPaymentOpt = paymentDetailsRepository.findByOrderId(savedOrder.getId());
        assertThat(retrievedPaymentOpt).isPresent();
        
        DomainEntityPaymentDetails retrievedPayment = retrievedPaymentOpt.get();
        logger.info("Retrieved payment details: {}", retrievedPayment);
        
        assertAll(
            "Payment details verification",
            () -> {
                assertThat(retrievedPayment.getOrderId()).isEqualTo(savedOrder.getId());
                logger.info("✓ Payment order ID matches: {}", retrievedPayment.getOrderId());
            },
            () -> {
                assertThat(retrievedPayment.getPaymentMethod()).isEqualTo("CREDIT_CARD");
                logger.info("✓ Payment method matches: {}", retrievedPayment.getPaymentMethod());
            },
            () -> {
                assertThat(retrievedPayment.getAmount().getAmount()).isEqualByComparingTo(totalAmount);
                logger.info("✓ Payment amount matches: {}", retrievedPayment.getAmount());
            },
            () -> {
                assertThat(retrievedPayment.getStatus().getStatus()).isEqualTo(SharedPaymentStatusEnum.SUCCESS);
                logger.info("✓ Payment status matches: {}", retrievedPayment.getStatus().getStatus());
            },
            () -> {
                assertNotNull(retrievedPayment.getPaymentId());
                logger.info("✓ Payment ID is set: {}", retrievedPayment.getPaymentId());
            }
        );
        
        // Then: Verify repository existence check
        boolean orderExists = orderRepositoryImpl.existsById(savedOrder.getId());
        assertTrue(orderExists, "Order should exist in database");
        logger.info("✓ Order existence verified: {}", orderExists);
        
        logger.info("=== ORDER REPOSITORY INTEGRATION TEST COMPLETED SUCCESSFULLY ====");
        
        // Print logs for analysis
        String logOutput = output.toString();
        System.out.println("\n" + "=".repeat(100));
        System.out.println("ORDER REPOSITORY INTEGRATION TEST LOGS:");
        System.out.println("=".repeat(100));
        System.out.println(logOutput);
        System.out.println("=".repeat(100) + "\n");
    }
    
    private DomainEntityOrder createTestOrder() {
        DomainEntityOrder order = new DomainEntityOrder();
        order.setUserId(123L);
        order.setPaymentMethod("CREDIT_CARD");
        order.setPaymentStatus(DomainValuePaymentStatus.of(SharedPaymentStatusEnum.SUCCESS));
        order.setReservationState(DomainValueOrderStatus.of(SharedOrderStatusEnum.COMPLETED));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
    
    private DomainEntityOrderItem createTestOrderItem(Long productId, Integer quantity, BigDecimal price) {
        DomainEntityOrderItem item = new DomainEntityOrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPrice(DomainValueMoney.of(price, "USD"));
        return item;
    }
    
    private DomainEntityPaymentDetails createTestPaymentDetails(Long orderId) {
        DomainEntityPaymentDetails paymentDetails = new DomainEntityPaymentDetails();
        paymentDetails.setPaymentId("payment-test-" + System.currentTimeMillis());
        paymentDetails.setOrderId(orderId);
        paymentDetails.setPaymentMethod("CREDIT_CARD");
        paymentDetails.setAmount(DomainValueMoney.of(new BigDecimal("99.97"), "USD"));
        paymentDetails.setStatus(DomainValuePaymentStatus.of(SharedPaymentStatusEnum.SUCCESS));
        return paymentDetails;
    }
}