package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.ports.DomainOutputPortInventoryService;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceException;
import ai.shreds.proto.inventory.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InfrastructureInventoryServiceImpl implements DomainOutputPortInventoryService {

    private final ManagedChannel channel;
    private final InventoryServiceGrpc.InventoryServiceBlockingStub blockingStub;

    @Value("${inventory.service.host:localhost}")
    private String host;

    @Value("${inventory.service.port:9090}")
    private int port;

    public InfrastructureInventoryServiceImpl() {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = InventoryServiceGrpc.newBlockingStub(channel);
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Inventory Service gRPC client for {}:{}", host, port);
    }

    @PreDestroy
    public void shutdown() {
        try {
            log.info("Shutting down Inventory Service gRPC channel");
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error during gRPC channel shutdown", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    @Retryable(value = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public boolean reserveItems(List<DomainEntityOrderItem> items) {
        try {
            log.debug("Attempting to reserve {} items", items.size());
            ReserveRequest request = createReserveRequest(items);
            ReserveResponse response = blockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .reserveItems(request);

            if (response.getSuccess()) {
                log.info("Successfully reserved {} items", items.size());
                return true;
            } else {
                log.warn("Failed to reserve items: {}", response.getMessage());
                return false;
            }

        } catch (StatusRuntimeException e) {
            handleGrpcException("reserve", e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during inventory reservation", e);
            throw new InfrastructureServiceException("Failed to reserve inventory", "INVENTORY", e);
        }
    }

    @Override
    @Retryable(value = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void releaseItems(List<DomainEntityOrderItem> items) {
        try {
            log.debug("Attempting to release {} items", items.size());
            ReleaseRequest request = createReleaseRequest(items);
            ReleaseResponse response = blockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .releaseItems(request);

            if (!response.getSuccess()) {
                log.error("Failed to release items: {}", response.getMessage());
                throw new InfrastructureServiceException(
                        "Failed to release inventory: " + response.getMessage(),
                        "INVENTORY",
                        null
                );
            }
            log.info("Successfully released {} items", items.size());

        } catch (StatusRuntimeException e) {
            handleGrpcException("release", e);
        } catch (Exception e) {
            log.error("Unexpected error during inventory release", e);
            throw new InfrastructureServiceException("Failed to release inventory", "INVENTORY", e);
        }
    }

    private ReserveRequest createReserveRequest(List<DomainEntityOrderItem> items) {
        List<ReserveItem> reserveItems = items.stream()
                .map(item -> ReserveItem.newBuilder()
                        .setProductId(item.getProductId())
                        .setQuantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return ReserveRequest.newBuilder()
                .addAllItems(reserveItems)
                .build();
    }

    private ReleaseRequest createReleaseRequest(List<DomainEntityOrderItem> items) {
        List<ReleaseItem> releaseItems = items.stream()
                .map(item -> ReleaseItem.newBuilder()
                        .setProductId(item.getProductId())
                        .setQuantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return ReleaseRequest.newBuilder()
                .addAllItems(releaseItems)
                .build();
    }

    private void handleGrpcException(String operation, StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        log.error("gRPC error during {} operation: {} - {}", operation, status.getCode(), status.getDescription());

        switch (status.getCode()) {
            case UNAVAILABLE:
                throw new InfrastructureServiceException("Inventory service unavailable", "INVENTORY", e);
            case DEADLINE_EXCEEDED:
                throw new InfrastructureServiceException("Inventory service timeout", "INVENTORY", e);
            case INTERNAL:
                throw new InfrastructureServiceException("Internal inventory service error", "INVENTORY", e);
            default:
                throw new InfrastructureServiceException(
                        "Unexpected inventory service error: " + status.getCode(),
                        "INVENTORY",
                        e
                );
        }
    }
}
