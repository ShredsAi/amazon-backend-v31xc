package ai.shreds.domain.events;

import java.time.LocalDateTime;

public interface DomainEvent {
    LocalDateTime getOccurredOn();
    String getEventType();
}