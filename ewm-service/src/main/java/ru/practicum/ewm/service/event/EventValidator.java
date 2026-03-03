package ru.practicum.ewm.service.event;

import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.event.EventStateAction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static ru.practicum.dto.Formatter.toInstant;

public class EventValidator {

    /**
     * Validates whether a state transition is allowed based on the user role (admin or regular user).
     * For admin users, validates state actions; for regular users, validates the current state.
     *
     * @param action  the action to be performed on the event
     * @param state   the current state of the event
     * @param isAdmin flag indicating whether the user is an admin (true) or regular user (false)
     * @throws ConflictException if the state transition is invalid for the given role
     */
    public static void throwIfStateTransitionInvalid(EventStateAction action, EventState state, boolean isAdmin) {
        if (isAdmin) {
            throwIfStateActionInvalid(action, state);
        } else {
            throwIfStateInvalid(state);
        }
    }

    /**
     * Validates that the event date is at least a specified number of hours from the current moment.
     * Parses the date string to an Instant and compares it with the current time plus the specified hours.
     *
     * @param date        the event date string in format {@code yyyy-MM-dd HH:mm:ss}
     * @param amountToAdd the minimum number of hours that must elapse between now and the event date
     * @throws BadRequestException if the event date is earlier than the required minimum hours from now
     */
    public static void throwIfDateInvalid(String date, long amountToAdd) {
        Instant deadline = Instant.now().plus(amountToAdd, ChronoUnit.HOURS);
        Instant eventDate = toInstant(date);

        if (!eventDate.isAfter(deadline)) {
            throw new BadRequestException("Event date can`t be earlier than " + amountToAdd + " hours from current moment");

        }
    }

    /**
     * Validates that a regular user can modify an event based on its current state.
     * Users can only modify events that are in {@code PENDING} or {@code CANCELED} state.
     *
     * @param state the current state of the event
     * @throws ConflictException if the event is not in {@code PENDING} or {@code CANCELED} state
     */
    private static void throwIfStateInvalid(EventState state) {
        if (!state.equals(EventState.PENDING) && !state.equals(EventState.CANCELED)) {
            throw new ConflictException("Changes can be done only if event is PENDING or CANCELED");
        }
    }

    /**
     * Validates admin state actions against the current event state.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>{@code PUBLISH_EVENT} action can only be applied to events in {@code PENDING} state</li>
     *   <li>{@code REJECT_EVENT} action cannot be applied to events in {@code PUBLISHED} state</li>
     * </ul>
     *
     * @param stateAction the admin action to be performed on the event
     * @param state       the current state of the event
     * @throws ConflictException if the state action is invalid for the current event state
     */
    private static void throwIfStateActionInvalid(EventStateAction stateAction, EventState state) {
        if (stateAction == null) {
            return;
        }

        switch (stateAction) {
            case PUBLISH_EVENT:
                if (!state.equals(EventState.PENDING)) {
                    throw new ConflictException("Only PENDING events can be published");
                }
                break;
            case REJECT_EVENT:
                if (state.equals(EventState.PUBLISHED)) {
                    throw new ConflictException("PUBLISHED events cannot be rejected.");
                }
                break;
        }
    }
}
