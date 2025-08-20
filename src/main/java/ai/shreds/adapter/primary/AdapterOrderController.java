package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationInputPortCreateOrder;
import ai.shreds.shared.dtos.SharedCreateOrderRequest;
import ai.shreds.shared.dtos.SharedOrderResponse;
import ai.shreds.shared.dtos.SharedErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Validated
@Tag(name = "Order Management", description = "APIs for managing orders")
public class AdapterOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdapterOrderController.class);
    private final ApplicationInputPortCreateOrder createOrderPort;

    public AdapterOrderController(ApplicationInputPortCreateOrder createOrderPort) {
        this.createOrderPort = createOrderPort;
    }

    @Operation(summary = "Create a new order", description = "Creates a new order with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SharedOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SharedErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Order creation failed due to conflict",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SharedErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SharedErrorResponse.class)))
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SharedOrderResponse> createOrder(
            @Parameter(description = "Order creation request details", required = true)
            @Valid @RequestBody SharedCreateOrderRequest request) {
        
        logger.info("Received order creation request for user: {}", request.getUserId());
        
        SharedOrderResponse response = createOrderPort.createOrder(request);
        
        logger.info("Successfully created order with ID: {} for user: {}", 
                response.getId(), response.getUserId());
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}