package ai.shreds.domain.events;

import java.time.LocalDateTime;

/**
 * Base class for all domain events in the system.
 * Provides common functionality for event handling and tracking.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final String eventType;
    private final LocalDateTime occurredOn;
    private final String aggregateId;
    private final String aggregateType;

    protected DomainEvent(String eventType, String aggregateId, String aggregateType) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    /**
     * Returns a string representation of the event's data.
     * This should be implemented by concrete event classes to provide
     * event-specific data serialization.
     *
     * @return String representation of the event data
     */
    public abstract String getEventData();

    @Override
    public String toString() {
        return String.format("%s[id=%s, type=%s, aggregateId=%s, aggregateType=%s, occurredOn=%s]",
            getClass().getSimpleName(), eventId, eventType, aggregateId, aggregateType, occurredOn);
    }
}
