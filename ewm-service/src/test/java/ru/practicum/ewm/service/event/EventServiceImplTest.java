package ru.practicum.ewm.service.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.Formatter;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.EventMapper;
import ru.practicum.ewm.mappers.EventStateMapper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.specification.AdminEventSpecification;
import ru.practicum.ewm.repository.specification.PublicEventSpecification;
import ru.practicum.ewm.service.category.CategoryService;
import ru.practicum.ewm.service.user.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class EventServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private EventStateMapper eventStateMapper;

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private EventServiceImpl eventService;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;
    @Captor
    private ArgumentCaptor<EndpointHitDto> hitCaptor;

    private User user;
    private Category category;
    private Event event;
    private EventDtoRequest request;
    private final Long userId = 1L;
    private final Long eventId = 1L;
    private final Long categoryId = 1L;
    private final String ip = "192.168.0.1";

    @BeforeEach
    public void setUp() {
        Instant eventDate = Instant.now().plus(3, ChronoUnit.HOURS);

        user = new User();
        user.setId(userId);
        user.setName("Test User");

        category = new Category();
        category.setId(categoryId);
        category.setName("Test Category");

        event = Event.builder()
                .id(eventId)
                .initiator(user)
                .category(category)
                .state(EventState.PENDING)
                .lat(55.75)
                .lon(37.62)
                .title("Test Event")
                .annotation("Test Annotation")
                .description("Test Description")
                .eventDate(Instant.now().plus(3, ChronoUnit.HOURS))
                .participantLimit(0)
                .paid(false)
                .requestModeration(true)
                .build();


        request = EventDtoRequest.builder()
                .title("Updated Event")
                .annotation("Updated Annotation")
                .description("Updated Description")
                .eventDate(Formatter.format(eventDate))
                .location(new Location(55.75, 37.62))
                .category(categoryId)
                .paid(true)
                .participantLimit(100)
                .requestModeration(false)
                .build();
    }

    @Nested
    class FindingEntityById {

        @Test
        public void shouldReturnEventWhenExists() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            Event result = eventService.findEntityById(eventId);

            assertThat(result).isEqualTo(event);
            verify(eventRepository).findById(eventId);
        }

        @Test
        public void shouldThrowNotFoundExceptionWhenEventNotExists() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> eventService.findEntityById(eventId));
        }
    }

    @Nested
    class FindingAdminEvents {

        @Test
        public void shouldReturnEventsWithStats() {
            List<Long> users = List.of(userId);
            List<EventState> states = List.of(EventState.PENDING);
            List<Long> categories = List.of(categoryId);
            String rangeStart = "2020-01-01 00:00:00";
            String rangeEnd = "2030-01-01 00:00:00";
            Integer from = 0;
            Integer size = 10;

            EventFullDto fullDto = new EventFullDto();
            fullDto.setId(eventId);

            EventRequestCount mockCount = mock(EventRequestCount.class);
            when(mockCount.getEventId()).thenReturn(eventId);
            when(mockCount.getCount()).thenReturn(5);

            when(eventRepository.findAll(any(AdminEventSpecification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(event)));
            when(eventMapper.toFullDto(event)).thenReturn(fullDto);
            when(requestRepository.countConfirmedRequestsByEventIds(anyList(), eq(ParticipationStatus.CONFIRMED)))
                    .thenReturn(List.of(mockCount));
            when(statsClient.getStats(any())).thenReturn(List.of(
                    ViewStatsDto.builder().uri("/events/1").hits(10L).build()
            ));

            List<EventFullDto> result = eventService.findAdminEvents(users, states, categories,
                    rangeStart, rangeEnd, from, size);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(eventId);
            assertThat(result.getFirst().getViews()).isEqualTo(10L);
            assertThat(result.getFirst().getConfirmedRequests()).isEqualTo(5);
        }

        @Test
        public void shouldThrowBadRequestWhenStartAfterEnd() {
            List<Long> users = List.of(userId);
            List<EventState> states = List.of(EventState.PENDING);
            List<Long> categories = List.of(categoryId);
            String rangeStart = "2030-01-01 00:00:00";
            String rangeEnd = "2020-01-01 00:00:00";
            Integer from = 0;
            Integer size = 10;

            assertThrows(BadRequestException.class, () -> eventService.findAdminEvents(users, states, categories,
                    rangeStart, rangeEnd, from, size));

        }
    }

    @Nested
    class FindingPublicEvents {

        @Test
        public void shouldReturnEventsWithStatsAndHit() {
            String text = "test";
            List<Long> categories = List.of(categoryId);
            Boolean paid = true;
            Instant rangeStart = Instant.now();
            Instant rangeEnd = Instant.now().plus(1, ChronoUnit.DAYS);
            boolean onlyAvailable = true;
            String sort = "EVENT_DATE";
            Integer from = 0;
            Integer size = 10;

            EventShortDto shortDto = new EventShortDto();
            shortDto.setId(eventId);

            EventRequestCount mockCount = mock(EventRequestCount.class);
            when(mockCount.getEventId()).thenReturn(eventId);
            when(mockCount.getCount()).thenReturn(5);

            when(eventRepository.findAll(any(PublicEventSpecification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(event)));
            when(eventMapper.toShortDto(event)).thenReturn(shortDto);
            when(requestRepository.countConfirmedRequestsByEventIds(anyList(), eq(ParticipationStatus.CONFIRMED)))
                    .thenReturn(List.of(mockCount));
            when(statsClient.getStats(any())).thenReturn(List.of(
                    ViewStatsDto.builder().uri("/events/1").hits(10L).build()
            ));

            List<EventShortDto> result = eventService.findPublicEvents(text, categories, paid,
                    rangeStart, rangeEnd, onlyAvailable, sort, from, size, ip);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(eventId);
            assertThat(result.getFirst().getViews()).isEqualTo(10L);
            assertThat(result.getFirst().getConfirmedRequests()).isEqualTo(5);
            verify(statsClient).hit(any(EndpointHitDto.class));
        }

        @Test
        public void shouldSortByViewsWhenSortParamIsViews() {
            String sort = "VIEWS";
            Integer from = 0;
            Integer size = 10;

            Event event2 = new Event();
            event2.setId(2L);

            EventShortDto shortDto1 = new EventShortDto();
            shortDto1.setId(eventId);
            shortDto1.setViews(5L);

            EventShortDto shortDto2 = new EventShortDto();
            shortDto2.setId(2L);
            shortDto2.setViews(10L);

            EventRequestCount mockCount1 = mock(EventRequestCount.class);
            when(mockCount1.getEventId()).thenReturn(eventId);
            when(mockCount1.getCount()).thenReturn(3);

            EventRequestCount mockCount2 = mock(EventRequestCount.class);
            when(mockCount2.getEventId()).thenReturn(2L);
            when(mockCount2.getCount()).thenReturn(7);

            when(eventRepository.findAll(any(PublicEventSpecification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(event, event2)));
            when(eventMapper.toShortDto(event)).thenReturn(shortDto1);
            when(eventMapper.toShortDto(event2)).thenReturn(shortDto2);
            when(requestRepository.countConfirmedRequestsByEventIds(anyList(), eq(ParticipationStatus.CONFIRMED)))
                    .thenReturn(List.of(mockCount1, mockCount2));
            when(statsClient.getStats(any())).thenReturn(List.of(
                    ViewStatsDto.builder().uri("/events/1").hits(5L).build(),
                    ViewStatsDto.builder().uri("/events/2").hits(10L).build()
            ));

            List<EventShortDto> result = eventService.findPublicEvents(null, null, null,
                    null, null, false, sort, from, size, ip);

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getId()).isEqualTo(2L);
            assertThat(result.getFirst().getViews()).isEqualTo(10L);
            assertThat(result.get(1).getId()).isEqualTo(1L);
            assertThat(result.get(1).getViews()).isEqualTo(5L);
        }

        @Test
        public void shouldThrowBadRequestWhenStartAfterEnd() {
            Instant rangeStart = Instant.now().plus(1, ChronoUnit.DAYS);
            Instant rangeEnd = Instant.now();

            assertThrows(BadRequestException.class, () -> eventService.findPublicEvents(null, null, null,
                    rangeStart, rangeEnd, false, null, 0, 10, ip));
        }
    }

    @Nested
    class FindingPublicEvent {

        @Test
        public void shouldReturnEventWithStatsAndHit() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            event.setState(EventState.PUBLISHED);

            EventFullDto fullDto = new EventFullDto();
            fullDto.setId(eventId);

            when(eventMapper.toFullDto(event)).thenReturn(fullDto);
            when(requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED))
                    .thenReturn(5);
            when(statsClient.getStats(any())).thenReturn(List.of(
                    ViewStatsDto.builder().uri("/events/1").hits(10L).build()
            ));

            EventFullDto result = eventService.findPublicEvent(eventId, ip);

            assertThat(result.getId()).isEqualTo(eventId);
            assertThat(result.getViews()).isEqualTo(10L);
            assertThat(result.getConfirmedRequests()).isEqualTo(5);
            verify(statsClient).hit(hitCaptor.capture());
            assertThat(hitCaptor.getValue().getUri()).isEqualTo("/events/1");
        }

        @Test
        public void shouldThrowNotFoundExceptionWhenEventNotPublished() {
            event.setState(EventState.PENDING);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            assertThrows(NotFoundException.class, () -> eventService.findPublicEvent(eventId, ip));
        }
    }

    @Nested
    class FindingEventsByUserId {

        @Test
        public void shouldReturnEventsForUser() {
            Integer from = 0;
            Integer size = 10;

            EventShortDto shortDto = new EventShortDto();
            shortDto.setId(eventId);

            doNothing().when(userService).throwIfUserNotFound(userId);
            when(eventRepository.findByInitiatorId(eq(userId), any(PageRequest.class)))
                    .thenReturn(List.of(event));
            when(eventMapper.toShortDto(event)).thenReturn(shortDto);

            List<EventShortDto> result = eventService.findEventsByUserId(userId, from, size);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(eventId);
            verify(userService).throwIfUserNotFound(userId);
        }
    }

    @Nested
    class FindingEventById {

        @Test
        public void shouldReturnEventWhenUserIsInitiator() {
            doNothing().when(userService).throwIfUserNotFound(userId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            EventFullDto fullDto = new EventFullDto();
            fullDto.setId(eventId);
            when(eventMapper.toFullDto(event)).thenReturn(fullDto);

            EventFullDto result = eventService.findEventById(userId, eventId);

            assertThat(result.getId()).isEqualTo(eventId);
        }

        @Test
        public void shouldThrowNotFoundExceptionWhenEventNotExists() {
            doNothing().when(userService).throwIfUserNotFound(userId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> eventService.findEventById(userId, eventId));
        }

        @Test
        public void shouldThrowBadRequestWhenUserNotInitiator() {
            doNothing().when(userService).throwIfUserNotFound(2L);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            assertThrows(BadRequestException.class, () -> eventService.findEventById(2L, eventId));
        }
    }

    @Nested
    class AddingEvent {

        @Test
        public void shouldSaveEventSuccessfully() {
            when(userService.findEntityById(userId)).thenReturn(user);
            when(categoryService.findEntityById(categoryId)).thenReturn(category);
            when(eventMapper.toEvent(request)).thenReturn(event);
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            EventFullDto fullDto = new EventFullDto();
            fullDto.setId(eventId);
            when(eventMapper.toFullDto(event)).thenReturn(fullDto);

            EventFullDto result = eventService.addEvent(userId, request);

            assertThat(result.getId()).isEqualTo(eventId);
            verify(eventRepository).save(eventCaptor.capture());
            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getState()).isEqualTo(EventState.PENDING);
            assertThat(savedEvent.getInitiator()).isEqualTo(user);
            assertThat(savedEvent.getCategory()).isEqualTo(category);
        }

        @Test
        public void shouldSetDefaultValuesWhenNull() {
            Event newEvent = new Event();
            newEvent.setParticipantLimit(null);
            newEvent.setPaid(null);
            newEvent.setRequestModeration(null);

            when(userService.findEntityById(userId)).thenReturn(user);
            when(categoryService.findEntityById(categoryId)).thenReturn(category);
            when(eventMapper.toEvent(request)).thenReturn(newEvent);
            when(eventRepository.save(any(Event.class))).thenReturn(newEvent);
            when(eventMapper.toFullDto(any())).thenReturn(new EventFullDto());

            eventService.addEvent(userId, request);

            verify(eventRepository).save(eventCaptor.capture());
            Event savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getParticipantLimit()).isEqualTo(0);
            assertThat(savedEvent.getPaid()).isFalse();
            assertThat(savedEvent.getRequestModeration()).isTrue();
        }

        @Test
        public void shouldThrowBadRequestWhenEventDateTooSoon() {
            request.setEventDate(Formatter.format(Instant.EPOCH));
            assertThrows(BadRequestException.class, () -> eventService.addEvent(userId, request));
        }

        @Test
        public void shouldThrowConflictExceptionOnDataIntegrityViolation() {
            when(userService.findEntityById(userId)).thenReturn(user);
            when(categoryService.findEntityById(categoryId)).thenReturn(category);
            when(eventMapper.toEvent(request)).thenReturn(event);
            when(eventRepository.save(any(Event.class))).thenThrow(DataIntegrityViolationException.class);

            assertThrows(ConflictException.class, () -> eventService.addEvent(userId, request));
        }
    }

    @Nested
    class PatchingEventByUser {

        @Test
        public void shouldUpdateEventSuccessfully() {
            request.setStateAction(EventStateAction.SEND_TO_REVIEW);

            doNothing().when(userService).throwIfUserNotFound(userId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(categoryService.findEntityById(categoryId)).thenReturn(category);
            when(eventStateMapper.mapUserEventAction(EventStateAction.SEND_TO_REVIEW))
                    .thenReturn(EventState.PENDING);
            doNothing().when(eventMapper).merge(event, request);
            when(eventRepository.save(event)).thenReturn(event);
            when(eventMapper.toFullDto(event)).thenReturn(new EventFullDto());

            eventService.patchEvent(userId, eventId, request);

            verify(eventRepository).save(event);
            verify(eventMapper).merge(event, request);
        }

        @Test
        public void shouldUpdateEventSuccessfullyNoParams() {
            EventDtoRequest request = EventDtoRequest.builder()
                    .title("title")
                    .build();

            doNothing().when(userService).throwIfUserNotFound(userId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            doNothing().when(eventMapper).merge(event, request);
            when(eventRepository.save(event)).thenReturn(event);
            when(eventMapper.toFullDto(event)).thenReturn(new EventFullDto());

            eventService.patchEvent(userId, eventId, request);

            verify(eventRepository).save(event);
            verify(eventMapper).merge(event, request);
        }

        @Test
        public void shouldThrowNotFoundExceptionWhenEventNotExists() {
            doNothing().when(userService).throwIfUserNotFound(userId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> eventService.patchEvent(userId, eventId, request));
        }

        @Test
        public void shouldThrowBadRequestWhenDateTooSoon() {
            request.setEventDate(Formatter.format(Instant.now().plus(1, ChronoUnit.HOURS)));

            doNothing().when(userService).throwIfUserNotFound(userId);
            assertThrows(BadRequestException.class, () -> eventService.patchEvent(userId, eventId, request));
        }

        @Test
        public void shouldCatchDataIntegrityViolationExceptionANdThrowConflict() {
            when(eventRepository.findById(userId)).thenThrow(new DataIntegrityViolationException(""));

            assertThrows(ConflictException.class, () -> eventService.patchEvent(userId, eventId, request));
        }
    }

    @Nested
    class PatchingEventByAdmin {

        @Test
        public void shouldPublishEventSuccessfully() {
            request.setStateAction(EventStateAction.PUBLISH_EVENT);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(categoryService.findEntityById(categoryId)).thenReturn(category);
            when(eventStateMapper.mapAdminEventAction(EventStateAction.PUBLISH_EVENT))
                    .thenReturn(EventState.PUBLISHED);
            doNothing().when(eventMapper).merge(event, request);
            when(eventRepository.save(event)).thenReturn(event);
            when(eventMapper.toFullDto(event)).thenReturn(new EventFullDto());

            eventService.patchAdminEvent(eventId, request);

            verify(eventRepository).save(event);
            assertThat(event.getPublishedOn()).isNotNull();
            assertThat(event.getState()).isEqualTo(EventState.PUBLISHED);
        }

        @Test
        public void shouldRejectEventSuccessfully() {
            request.setStateAction(EventStateAction.REJECT_EVENT);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(categoryService.findEntityById(categoryId)).thenReturn(category);
            when(eventStateMapper.mapAdminEventAction(EventStateAction.REJECT_EVENT))
                    .thenReturn(EventState.CANCELED);
            doNothing().when(eventMapper).merge(event, request);
            when(eventRepository.save(event)).thenReturn(event);
            when(eventMapper.toFullDto(event)).thenReturn(new EventFullDto());

            eventService.patchAdminEvent(eventId, request);

            verify(eventRepository).save(event);
            assertThat(event.getState()).isEqualTo(EventState.CANCELED);
        }
    }

    @Nested
    class ThrowingIfEventNotFound {

        @Test
        public void shouldNotThrowWhenEventExists() {
            when(eventRepository.existsById(eventId)).thenReturn(true);

            eventService.throwIfEventNotFound(eventId);

            verify(eventRepository).existsById(eventId);
        }

        @Test
        public void shouldThrowNotFoundExceptionWhenEventNotExists() {
            when(eventRepository.existsById(eventId)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> eventService.throwIfEventNotFound(eventId));
        }
    }
}
