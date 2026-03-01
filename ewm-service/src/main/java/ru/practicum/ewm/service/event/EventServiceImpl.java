package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.Formatter;
import ru.practicum.dto.StatsRequest;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.EventMapper;
import ru.practicum.ewm.mappers.EventStateMapper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.specification.AdminEventSpecification;
import ru.practicum.ewm.repository.specification.PublicEventSpecification;
import ru.practicum.ewm.service.category.CategoryService;
import ru.practicum.ewm.service.user.UserService;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.dto.Formatter.toInstant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final long HOURS_BEFORE_START_USER = 2;
    private static final long HOURS_BEFORE_START_ADMIN = 1;
    private static final String APP_NAME = "ewm-main-service";

    private final UserService userService;
    private final CategoryService categoryService;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final EventStateMapper eventStateMapper;
    private final StatsClient statsClient;

    @Override
    public Event findEntityById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id " + id + " not found"));
    }

    @Override
    public List<EventFullDto> findAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                         String rangeStart, String rangeEnd, Integer from, Integer size) {

        Instant start = getRangeInstant(rangeStart);
        Instant end = getRangeInstant(rangeEnd);

        if (start != null && end != null) {
            if (start.isAfter(end)) {
                throw new BadRequestException("Start can`t be after end");
            }
        }

        AdminEventSpecification spec = new AdminEventSpecification(users, states, categories, start, end);

        return eventRepository.findAll(spec, PageRequest.of(from / size, size)).stream()
                .map(eventMapper::toFullDto)
                .toList();
    }

    @Override
    public List<EventShortDto> findPublicEvents(String text, List<Long> categories, Boolean paid,
                                                Instant rangeStart, Instant rangeEnd, boolean onlyAvailable,
                                                String sort, Integer from, Integer size, String ip) {
        // записываем в стату что кто то зашел на /events
        statsClient.hit(EndpointHitDto.builder()
                        .app(APP_NAME)
                        .uri("/events")
                        .ip(ip)
                        .timestamp(Formatter.format(Instant.now()))
                        .build());

        if (rangeStart != null && rangeEnd != null) {
            if (rangeStart.isAfter(rangeEnd)) {
                throw new BadRequestException("Start can`t be after end");
            }
        }

        PublicEventSpecification spec = new PublicEventSpecification(
                onlyAvailable, rangeStart, rangeEnd, paid, categories, text);

        // достаём события из БД по фильтрам с пагинацией
        List<Event> events = eventRepository.findAll(spec, PageRequest.of(from / size, size)).getContent();

        // собираем id всех событий чтобы одним запросом получить просмотры
        List<Long> ids = events.stream().map(Event::getId).toList();

        // идём в сервис статистики и получаем { eventId - количество просмотров }
        Map<Long, Long> viewsMap = getViewsMap(ids);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(),
                            ParticipationStatus.CONFIRMED));
                    return dto;
                })
                .toList();

        // сортировка: по VIEWS в коде, по EVENT_DATE через БД (уже по умолчанию)
        if ("VIEWS".equals(sort)) {
            result = result.stream()
                    .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                    .toList();
        }

        return result;
    }

    @Override
    public EventFullDto findPublicEvent(Long eventId, String ip) {
        statsClient.hit(EndpointHitDto.builder()
                .app(APP_NAME)
                .uri("/events/" + eventId)
                .ip(ip)
                .timestamp(Formatter.format(Instant.now()))
                .build());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие не найдено");
        }

        EventFullDto dto = eventMapper.toFullDto(event);
        int confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);
        // getViewsMap принимает List а не один id — оборачиваем
        dto.setViews(getViewsMap(List.of(eventId)).getOrDefault(eventId, 0L));
        dto.setConfirmedRequests(confirmedRequests);
        return dto;
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

    // Map<Long, Long> - 1ый это eventId, 2ой кол-во просмотров
    private Map<Long, Long> getViewsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<String> uris = eventIds.stream().map(id -> "/events/" + id).toList();

        StatsRequest statsRequest = StatsRequest.builder()
                .start("1970-01-01 00:00:00")
                .end(Formatter.format(Instant.now().plusSeconds(1)))
                .uris(uris)
                .unique(true)
                .build();

        List<ViewStatsDto> stats = statsClient.getStats(statsRequest);
        if (stats == null || stats.isEmpty()) return Map.of();

        return stats.stream()
                // stat - номер ивента
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.getUri().replace("/events/", "")),
                        ViewStatsDto::getHits, // "значением будет поле hits из dto
                        Long::sum // если такой ключ уже есть — сложи старое и новое значение
                ));
    }
}
