package ru.practicum.ewm.service.event;

import org.junit.jupiter.api.Test;
import ru.practicum.dto.Formatter;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.event.EventStateAction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EventValidatorTest {

    @Test
    public void shouldNotThrowWhenUserTriesToModifyPendingEvent() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(null, EventState.PENDING, false));
    }

    @Test
    public void shouldNotThrowWhenUserTriesToModifyCanceledEvent() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(null, EventState.CANCELED, false));
    }

    @Test
    public void shouldThrowIfStateInvalidWhenUserTriesToModifyPublishedEvent() {
        assertThrows(ConflictException.class,
                () -> EventValidator.throwIfStateTransitionInvalid(null, EventState.PUBLISHED, false));
    }

    @Test
    public void shouldThrowIfAdminPublishesNonPendingEvent() {
        assertThrows(ConflictException.class,
                () -> EventValidator.throwIfStateTransitionInvalid(EventStateAction.PUBLISH_EVENT, EventState.PUBLISHED, true));
    }

    @Test
    public void shouldNotThrowIfAdminPublishesPendingEvent() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(EventStateAction.PUBLISH_EVENT, EventState.PENDING, true));
    }

    @Test
    public void shouldThrowIfAdminRejectsPublishedEvent() {
        assertThrows(ConflictException.class,
                () -> EventValidator.throwIfStateTransitionInvalid(EventStateAction.REJECT_EVENT, EventState.PUBLISHED, true));
    }

    @Test
    public void shouldNotThrowIfAdminRejectsPendingEvent() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(EventStateAction.REJECT_EVENT, EventState.PENDING, true));
    }

    @Test
    public void shouldNotThrowIfAdminActionIsNull() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(null, EventState.PENDING, true));
    }

    @Test
    public void shouldNotThrowIfDateIsAfterDeadline() {
        String futureDate = Formatter.format(Instant.now().plus(3, ChronoUnit.HOURS));
        assertDoesNotThrow(() -> EventValidator.throwIfDateInvalid(futureDate, 2));
    }

    @Test
    public void shouldThrowIfDateIsBeforeDeadline() {
        String pastDate = Formatter.format(Instant.now().plus(1, ChronoUnit.HOURS));
        assertThrows(BadRequestException.class, () -> EventValidator.throwIfDateInvalid(pastDate, 2));
    }

    @Test
    public void shouldThrowIfDateIsExactlyDeadline() {
        Instant deadline = Instant.now().plus(2, ChronoUnit.HOURS);
        String exactDate = Formatter.format(deadline);
        assertThrows(BadRequestException.class, () -> EventValidator.throwIfDateInvalid(exactDate, 2));
    }
}