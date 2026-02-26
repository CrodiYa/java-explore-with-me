package ru.practicum.ewm.service.event;

import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.event.EventDtoRequest;
import ru.practicum.ewm.model.event.EventFullDto;
import ru.practicum.ewm.model.event.EventShortDto;
import ru.practicum.ewm.model.event.EventState;

import java.util.List;

public interface EventService {

    /**
     * Retrieves a paginated list of events based on specified filters.
     * This method is used by administrators.
     *
     * @param users      list of user ids to filter events by initiator (optional, can be null or empty)
     * @param states     list of event states to filter by (optional, can be null or empty)
     * @param categories list of category IDs to filter by (optional, can be null or empty)
     * @param rangeStart start date-time in format {@code yyyy-MM-dd HH:mm:ss}
     *                   for filtering events after this date (optional)
     * @param rangeEnd   end date-time in format {@code yyyy-MM-dd HH:mm:ss}
     *                   for filtering events before this date (optional)
     * @param from       the index of the first element to retrieve (0-based)
     * @param size       the number of elements to retrieve
     * @return list of events {@link EventFullDto} matching the specified criteria
     */
    List<EventFullDto> findEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                  String rangeStart, String rangeEnd,
                                  Integer from, Integer size);


    /**
     * Retrieves a paginated list of events created by a specific user.
     *
     * @param userId id of the user whose events to retrieve
     * @param from   the index of the first element to retrieve (0-based)
     * @param size   the number of elements to retrieve
     * @return user`s list of events {@link EventShortDto}
     * @throws NotFoundException if user not found
     */
    List<EventShortDto> findEventsByUserId(Long userId, Integer from, Integer size);

    /**
     * Retrieves detailed information about a specific event for a user.
     *
     * @param userId  id of the user requesting the event
     * @param eventId id of the event to retrieve
     * @return full information about event {@link EventFullDto}
     * @throws NotFoundException if user or event not found
     */
    EventFullDto findEventById(Long userId, Long eventId);

    /**
     * Creates a new event for the specified user.
     * The event is created with {@code PENDING} state by default.
     * Validates that the event date is at least two hours in the future.
     *
     * @param userId  id of the user creating the event
     * @param request DTO containing event details
     * @return full event {@link EventFullDto}
     * @throws NotFoundException   if user or category not found
     * @throws BadRequestException if event date is invalid
     * @throws ConflictException   if a data integrity violation occurs
     */
    EventFullDto addEvent(Long userId, EventDtoRequest request);

    /**
     * Updates an existing event for the specified user.
     * Only events in {@code PENDING} or {@code CANCELED} state can be updated.
     * Validates the new event date if provided.
     *
     * @param userId  id of the user updating the event
     * @param eventId id of the event to update
     * @param request DTO containing the fields to update
     * @return full updated event {@link EventFullDto}
     * @throws NotFoundException   if the user, event, or category not found
     * @throws BadRequestException if date validation fails
     * @throws ConflictException   if the event cannot be updated or a data integrity violation occurs
     */
    EventFullDto patchEvent(Long userId, Long eventId, EventDtoRequest request);

    /**
     * Updates an existing event by an administrator.
     * Admin can publish or reject events and has different validation rules than regular users.
     *
     * @param eventId id of the event to update
     * @param request DTO containing the fields to update
     * @return full updated event {@link EventFullDto}
     * @throws NotFoundException   if the event or category not found
     * @throws BadRequestException if date validation fails
     * @throws ConflictException   if the event cannot be updated or a data integrity violation occurs
     */
    EventFullDto patchAdminEvent(Long eventId, EventDtoRequest request);

    /**
     * Checks if an event with the specified id exists.
     * Throws an exception if the event is not found.
     *
     * @param eventId id of the event to check
     * @throws NotFoundException if the event with the given id does not exist
     */
    void throwIfEventNotFound(Long eventId);
}
