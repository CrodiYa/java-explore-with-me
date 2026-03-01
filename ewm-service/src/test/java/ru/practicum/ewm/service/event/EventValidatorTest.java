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

class EventValidatorTest {

    @Test
    void shouldNotThrowWhenUserTriesToModifyPendingEvent() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(null, EventState.PENDING, false));
    }

    @Test
    void shouldNotThrowWhenUserTriesToModifyCanceledEvent() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(null, EventState.CANCELED, false));
    }

    @Test
    void shouldThrowIfStateInvalidWhenUserTriesToModifyPublishedEvent() {
        assertThrows(ConflictException.class,
                () -> EventValidator.throwIfStateTransitionInvalid(null, EventState.PUBLISHED, false));
    }

    @Test
    void shouldThrowIfAdminPublishesNonPendingEvent() {
        assertThrows(ConflictException.class,
                () -> EventValidator.throwIfStateTransitionInvalid(EventStateAction.PUBLISH_EVENT, EventState.PUBLISHED, true));
    }

    @Test
    void shouldNotThrowIfAdminPublishesPendingEvent() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(EventStateAction.PUBLISH_EVENT, EventState.PENDING, true));
    }

    @Test
    void shouldThrowIfAdminRejectsPublishedEvent() {
        assertThrows(ConflictException.class,
                () -> EventValidator.throwIfStateTransitionInvalid(EventStateAction.REJECT_EVENT, EventState.PUBLISHED, true));
    }

    @Test
    void shouldNotThrowIfAdminRejectsPendingEvent() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(EventStateAction.REJECT_EVENT, EventState.PENDING, true));
    }

    @Test
    void shouldNotThrowIfAdminActionIsNull() {
        assertDoesNotThrow(() -> EventValidator.throwIfStateTransitionInvalid(null, EventState.PENDING, true));
    }

    @Test
    void shouldNotThrowIfDateIsAfterDeadline() {
        String futureDate = Formatter.format(Instant.now().plus(3, ChronoUnit.HOURS));
        assertDoesNotThrow(() -> EventValidator.throwIfDateInvalid(futureDate, 2));
    }

    @Test
    void shouldThrowIfDateIsBeforeDeadline() {
        String pastDate = Formatter.format(Instant.now().plus(1, ChronoUnit.HOURS));
        assertThrows(BadRequestException.class, () -> EventValidator.throwIfDateInvalid(pastDate, 2));
    }

    @Test
    void shouldThrowIfDateIsExactlyDeadline() {
        Instant deadline = Instant.now().plus(2, ChronoUnit.HOURS);
        String exactDate = Formatter.format(deadline);
        assertThrows(BadRequestException.class, () -> EventValidator.throwIfDateInvalid(exactDate, 2));
    }
}