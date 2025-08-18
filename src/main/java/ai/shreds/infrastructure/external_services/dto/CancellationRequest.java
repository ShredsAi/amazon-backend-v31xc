package ai.shreds.infrastructure.external_services.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationRequest {
    private String orderId;
    private List<ReservationItem> items;
}