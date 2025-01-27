package ai.shreds.shared.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response structure")
public class SharedErrorResponse {

    @Schema(description = "Error message describing what went wrong", 
           example = "Invalid order data provided")
    private String message;

    @Schema(description = "Error code for the specific error type", 
           example = "ORDER_VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "Timestamp when the error occurred", 
           example = "2023-10-01T10:15:30.000+0000")
    private String timestamp;
}
