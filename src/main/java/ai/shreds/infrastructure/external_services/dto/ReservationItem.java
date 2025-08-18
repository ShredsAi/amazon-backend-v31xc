package ai.shreds.infrastructure.external_services.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationItem {
    private Long productId;
    private Integer quantity;
}