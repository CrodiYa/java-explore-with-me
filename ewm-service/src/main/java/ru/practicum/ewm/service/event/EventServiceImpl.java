package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.EventMapper;
import ru.practicum.ewm.mappers.EventStateMapper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.EventSpecification;
import ru.practicum.ewm.service.category.CategoryService;
import ru.practicum.ewm.service.user.UserService;

import java.time.Instant;
import java.util.List;

import static ru.practicum.dto.Formatter.toInstant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final long HOURS_BEFORE_START_USER = 2;
    private static final long HOURS_BEFORE_START_ADMIN = 1;

    private final UserService userService;
    private final CategoryService categoryService;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventStateMapper eventStateMapper;

    @Override
    public Event findEntityById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id " + id + " not found"));
    }

    @Override
    public List<EventFullDto> findEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                         String rangeStart, String rangeEnd, Integer from, Integer size) {

        Instant start = getRangeInstant(rangeStart);
        Instant end = getRangeInstant(rangeEnd);

        if (start != null && end != null) {
            if (start.isAfter(end)) {
                throw new BadRequestException("Start can`t be after end");
            }
        }

        EventSpecification spec = new EventSpecification(users, states, categories, start, end);

        return eventRepository.findAll(spec, PageRequest.of(from / size, size)).stream()
                .map(eventMapper::toFullDto)
                .toList();
    }

    @Override
    public List<EventShortDto> findEventsByUserId(Long userId, Integer from, Integer size) {
        userService.throwIfUserNotFound(userId);

        return eventRepository.findByInitiatorId(userId, PageRequest.of(from / size, size)).stream()
                .map(eventMapper::toShortDto)
                .toList();
    }

    @Override
    public EventFullDto findEventById(Long userId, Long eventId) {
        userService.throwIfUserNotFound(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new BadRequestException("UserId must match initiatorId");
        }

        return eventMapper.toFullDto(event);
    }

    @Override
    public EventFullDto addEvent(Long userId, EventDtoRequest request) {
        try {
            EventValidator.throwIfDateInvalid(request.getEventDate(), HOURS_BEFORE_START_USER);

            User user = userService.findEntityById(userId);
            Category category = categoryService.findEntityById(request.getCategory());

            Event event = eventMapper.toEvent(request);

            if (event.getParticipantLimit() == null) event.setParticipantLimit(0);
            if (event.getPaid() == null) event.setPaid(false);
            if (event.getRequestModeration() == null) event.setRequestModeration(true);

            event.setInitiator(user);
            event.setCategory(category);
            event.setState(EventState.PENDING);

            Event saved = eventRepository.save(event);
            log.info("Successfully saved event with id: {}", saved.getId());
            return eventMapper.toFullDto(saved);

        } catch (DataIntegrityViolationException e) {
            log.debug("Conflict during saving event [{}]", request, e);
            throw new ConflictException("Conflict with another event");
        }
    }

    @Override
    public EventFullDto patchEvent(Long userId, Long eventId, EventDtoRequest request) {
        userService.throwIfUserNotFound(userId);
        return patchEvent(eventId, request, HOURS_BEFORE_START_USER, false);
    }

    @Override
    public EventFullDto patchAdminEvent(Long eventId, EventDtoRequest request) {
        return patchEvent(eventId, request, HOURS_BEFORE_START_ADMIN, true);
    }

    @Override
    public void throwIfEventNotFound(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with id " + eventId + " not found");
        }
    }

    /**
     * Internal method that handles both user and admin event patching operations.
     *
     * @param eventId          id of the event to update
     * @param request          DTO containing the fields to update
     * @param hoursBeforeStart minimum number of hours required before the event starts
     * @param isAdmin          flag indicating whether the operation is performed by an admin
     * @return full updated event {@link EventFullDto}
     * @throws NotFoundException   if the event or category not found
     * @throws BadRequestException if date validation fails
     * @throws ConflictException   if state transition is invalid or a data integrity violation occurs
     */
    private EventFullDto patchEvent(Long eventId, EventDtoRequest request, long hoursBeforeStart, boolean isAdmin) {
        try {
            if (request.getEventDate() != null) {
                EventValidator.throwIfDateInvalid(request.getEventDate(), hoursBeforeStart);
            }

            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));

            EventStateAction action = request.getStateAction();
            EventValidator.throwIfStateTransitionInvalid(action, event.getState(), isAdmin);

            if (action != null) {
                EventState newState = isAdmin
                        ? eventStateMapper.mapAdminEventAction(action)
                        : eventStateMapper.mapUserEventAction(action);

                if (EventState.PUBLISHED.equals(newState)) {
                    event.setPublishedOn(Instant.now());
                }
                if (newState != null) {
                    event.setState(newState);
                }
            }

            if (request.getCategory() != null) {
                Category category = categoryService.findEntityById(request.getCategory());
                event.setCategory(category);
            }

            eventMapper.merge(event, request);

            Event patched = eventRepository.save(event);

            log.info("Successfully patched event with id: {}", patched.getId());
            return eventMapper.toFullDto(patched);

        } catch (DataIntegrityViolationException e) {
            log.debug("Conflict during patching event [{}]", request, e);
            throw new ConflictException("Conflict with another event");
        }
    }

    private Instant getRangeInstant(String date) {
        if (date != null) {
            return toInstant(date);
        }

        return null;
    }


}
