package ai.shreds.domain.entities;

import ai.shreds.domain.events.DomainEvent;
import ai.shreds.domain.events.OrderPaymentStatusChangedEvent;
import ai.shreds.domain.events.OrderReservationStateChangedEvent;
import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValueOrderStatus;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DomainEntityOrder {

    private Long id;
    private Long userId;
    private DomainValueMoney totalAmount;
    private DomainValuePaymentStatus paymentStatus;
    private DomainValueOrderStatus reservationState;
    private LocalDateTime createdAt;
    private List<DomainEntityOrderItem> items;
    private List<DomainEvent> domainEvents;

    public DomainEntityOrder() {
        this.items = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.paymentStatus = new DomainValuePaymentStatus(SharedPaymentStatusEnum.PENDING);
        this.reservationState = new DomainValueOrderStatus(SharedOrderStatusEnum.PENDING);
        this.totalAmount = new DomainValueMoney(BigDecimal.ZERO, "USD");
    }

    // Rest of the code remains the same...
}