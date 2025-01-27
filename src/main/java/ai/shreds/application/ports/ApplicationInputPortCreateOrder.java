package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedCreateOrderRequest;
import ai.shreds.shared.dtos.SharedOrderResponse;

public interface ApplicationInputPortCreateOrder {
    SharedOrderResponse createOrder(SharedCreateOrderRequest request);
}