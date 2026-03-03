package ru.practicum.ewm.controller.integration.priv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.Formatter;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.model.participation.ParticipationRequest;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "logging.level.root=ERROR",
        "spring.main.banner-mode=off"
})
public class PrivateEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ParticipationRequestRepository requestRepository;

    private User user;
    private User otherUser;
    private Category category;
    private Event event;
    private Event otherEvent;

    @BeforeEach
    public void setUp() {
        requestRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        user = userRepository.save(User.builder()
                .name("Test User")
                .email("user@test.com")
                .build());

        otherUser = userRepository.save(User.builder()
                .name("Other User")
                .email("other@test.com")
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Test Category")
                .build());

        Instant futureDate = Instant.now().plus(3, ChronoUnit.HOURS);

        event = eventRepository.save(Event.builder()
                .title("Test Event")
                .annotation("This is a test annotation that is long enough to pass validation")
                .description("This is a test description that is long enough to pass validation")
                .lat(55.75)
                .lon(37.62)
                .eventDate(futureDate)
                .category(category)
                .initiator(user)
                .paid(false)
                .state(EventState.PENDING)
                .participantLimit(10)
                .requestModeration(true)
                .build());

        otherEvent = eventRepository.save(Event.builder()
                .title("Other Event")
                .annotation("This is other annotation that is long enough to pass validation")
                .description("This is other description that is long enough to pass validation")
                .lat(55.76)
                .lon(37.63)
                .eventDate(futureDate)
                .category(category)
                .initiator(otherUser)
                .paid(true)
                .state(EventState.PUBLISHED)
                .participantLimit(5)
                .requestModeration(false)
                .build());
    }

    @Test
    public void shouldFindEventsByUserId() throws Exception {
        mockMvc.perform(get("/users/{userId}/events", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(event.getId().intValue())))
                .andExpect(jsonPath("$[0].title", is("Test Event")));
    }

    @Test
    public void shouldReturnEmptyListWhenUserHasNoEvents() throws Exception {
        User newUser = userRepository.save(User.builder()
                .name("New User")
                .email("new@test.com")
                .build());

        mockMvc.perform(get("/users/{userId}/events", newUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void shouldFindEventById() throws Exception {
        mockMvc.perform(get("/users/{userId}/events/{eventId}", user.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(event.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Test Event")))
                .andExpect(jsonPath("$.initiator.id", is(user.getId().intValue())));
    }

    @Test
    public void shouldReturnNotFoundWhenEventNotBelongToUser() throws Exception {
        mockMvc.perform(get("/users/{userId}/events/{eventId}", user.getId(), otherEvent.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAddEvent() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .title("New Event")
                .annotation("This is a new annotation that is long enough to pass validation")
                .description("This is a new description that is long enough to pass validation")
                .location(new Location(55.75, 37.62))
                .eventDate(Formatter.format(Instant.now().plus(5, ChronoUnit.HOURS)))
                .category(category.getId())
                .participantLimit(20)
                .paid(true)
                .requestModeration(true)
                .build();

        mockMvc.perform(post("/users/{userId}/events", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("New Event")))
                .andExpect(jsonPath("$.state", is("PENDING")))
                .andExpect(jsonPath("$.initiator.id", is(user.getId().intValue())));
    }

    @Test
    public void shouldReturnBadRequestWhenAddingEventWithInvalidDate() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .title("New Event")
                .annotation("This is a new annotation that is long enough to pass validation")
                .description("This is a new description that is long enough to pass validation")
                .location(new Location(55.75, 37.62))
                .eventDate(Formatter.format(Instant.now().plus(1, ChronoUnit.HOURS)))
                .category(category.getId())
                .participantLimit(20)
                .paid(true)
                .requestModeration(true)
                .build();

        mockMvc.perform(post("/users/{userId}/events", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldPatchEvent() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .title("Updated Title")
                .annotation("Updated annotation that is long enough to pass validation")
                .stateAction(EventStateAction.SEND_TO_REVIEW)
                .build();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(event.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.state", is("PENDING")));
    }

    @Test
    public void shouldCancelEvent() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .stateAction(EventStateAction.CANCEL_REVIEW)
                .build();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(event.getId().intValue())))
                .andExpect(jsonPath("$.state", is("CANCELED")));
    }

    @Test
    public void shouldReturnConflictWhenPatchingPublishedEvent() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .title("Updated Title")
                .build();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", otherUser.getId(), otherEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldGetRequestsForEvent() throws Exception {
        ParticipationRequest request = requestRepository.save(ParticipationRequest.builder()
                .event(event)
                .requester(otherUser)
                .status(ParticipationStatus.PENDING)
                .build());

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", user.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(request.getId().intValue())))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    public void shouldReturnNotFoundWhenGettingRequestsForNonOwnedEvent() throws Exception {
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", user.getId(), otherEvent.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldPatchRequestsConfirm() throws Exception {
        ParticipationRequest request1 = requestRepository.save(ParticipationRequest.builder()
                .event(event)
                .requester(otherUser)
                .status(ParticipationStatus.PENDING)
                .build());

        ParticipationRequest request2 = requestRepository.save(ParticipationRequest.builder()
                .event(event)
                .requester(userRepository.save(User.builder()
                        .name("Third User")
                        .email("third@test.com")
                        .build()))
                .status(ParticipationStatus.PENDING)
                .build());

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(request1.getId(), request2.getId()))
                .status(ParticipationStatus.CONFIRMED)
                .build();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(2)))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(0)));
    }

    @Test
    public void shouldPatchRequestsReject() throws Exception {
        ParticipationRequest request1 = requestRepository.save(ParticipationRequest.builder()
                .event(event)
                .requester(otherUser)
                .status(ParticipationStatus.PENDING)
                .build());

        ParticipationRequest request2 = requestRepository.save(ParticipationRequest.builder()
                .event(event)
                .requester(userRepository.save(User.builder()
                        .name("Third User")
                        .email("third@test.com")
                        .build()))
                .status(ParticipationStatus.PENDING)
                .build());

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(request1.getId(), request2.getId()))
                .status(ParticipationStatus.REJECTED)
                .build();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(0)))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(2)));
    }

    @Test
    public void shouldReturnConflictWhenConfirmingWithLimitReached() throws Exception {
        Event limitedEvent = eventRepository.save(Event.builder()
                .title("Limited Event")
                .annotation("This is a limited annotation that is long enough to pass validation")
                .description("This is a limited description that is long enough to pass validation")
                .lat(55.75)
                .lon(37.62)
                .eventDate(Instant.now().plus(3, ChronoUnit.HOURS))
                .category(category)
                .initiator(user)
                .paid(false)
                .state(EventState.PUBLISHED)
                .participantLimit(1)
                .requestModeration(true)
                .build());

        ParticipationRequest confirmedRequest = requestRepository.save(ParticipationRequest.builder()
                .event(limitedEvent)
                .requester(userRepository.save(User.builder()
                        .name("Third User")
                        .email("third@test.com")
                        .build()))
                .status(ParticipationStatus.CONFIRMED)
                .build());

        requestRepository.save(confirmedRequest);

        ParticipationRequest pendingRequest = requestRepository.save(ParticipationRequest.builder()
                .event(limitedEvent)
                .requester(userRepository.save(User.builder()
                        .name("Fourth User")
                        .email("fourth@test.com")
                        .build()))
                .status(ParticipationStatus.PENDING)
                .build());

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(pendingRequest.getId()))
                .status(ParticipationStatus.CONFIRMED)
                .build();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", user.getId(), limitedEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }
}
