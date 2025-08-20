package ai.shreds.infrastructure.external_services.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private boolean success;
    private String reservationId;
    private String message;

    public boolean isSuccess() {
        return success;
    }
}