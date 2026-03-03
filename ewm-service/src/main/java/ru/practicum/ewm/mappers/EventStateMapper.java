package ru.practicum.ewm.mappers;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.event.EventStateAction;

@Component
public class EventStateMapper {

    public EventState mapUserEventAction(EventStateAction action) {
        return switch (action) {
            case CANCEL_REVIEW -> EventState.CANCELED;
            case SEND_TO_REVIEW -> EventState.PENDING;
            case null, default -> null;
        };
    }

    public EventState mapAdminEventAction(EventStateAction action) {
        return switch (action) {
            case PUBLISH_EVENT -> EventState.PUBLISHED;
            case REJECT_EVENT -> EventState.CANCELED;
            case null, default -> null;
        };
    }
}