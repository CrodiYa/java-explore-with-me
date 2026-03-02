package ru.practicum.ewm.controller.integration.priv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.participation.ParticipationRequest;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
public class PrivateParticipationRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ParticipationRequestRepository requestRepository;

    private User requester;
    private User initiator;
    private Category category;
    private Event event;
    private ParticipationRequest request;

    @BeforeEach
    public void setUp() {
        requestRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        requester = userRepository.save(User.builder()
                .name("Requester")
                .email("requester@test.com")
                .build());

        initiator = userRepository.save(User.builder()
                .name("Initiator")
                .email("initiator@test.com")
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
                .initiator(initiator)
                .paid(false)
                .state(EventState.PUBLISHED)
                .participantLimit(10)
                .requestModeration(true)
                .build());

        request = requestRepository.save(ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .status(ParticipationStatus.PENDING)
                .build());
    }

    @Test
    public void shouldFindRequestsByRequesterId() throws Exception {
        mockMvc.perform(get("/users/{userId}/requests", requester.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(request.getId().intValue())))
                .andExpect(jsonPath("$[0].event", is(event.getId().intValue())))
                .andExpect(jsonPath("$[0].requester", is(requester.getId().intValue())))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    public void shouldReturnEmptyListWhenRequesterHasNoRequests() throws Exception {
        User newUser = userRepository.save(User.builder()
                .name("New User")
                .email("new@test.com")
                .build());

        mockMvc.perform(get("/users/{userId}/requests", newUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void shouldAddParticipationRequest() throws Exception {
        User newRequester = userRepository.save(User.builder()
                .name("New Requester")
                .email("newrequester@test.com")
                .build());

        mockMvc.perform(post("/users/{userId}/requests", newRequester.getId())
                        .param("eventId", event.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.event", is(event.getId().intValue())))
                .andExpect(jsonPath("$.requester", is(newRequester.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    public void shouldReturnConflictWhenAddingRequestToOwnEvent() throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", initiator.getId())
                        .param("eventId", event.getId().toString()))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldReturnConflictWhenAddingDuplicateRequest() throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", requester.getId())
                        .param("eventId", event.getId().toString()))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldReturnConflictWhenAddingRequestToUnpublishedEvent() throws Exception {
        Event unpublishedEvent = eventRepository.save(Event.builder()
                .title("Unpublished Event")
                .annotation("This is an unpublished annotation that is long enough to pass validation")
                .description("This is an unpublished description that is long enough to pass validation")
                .lat(55.75)
                .lon(37.62)
                .eventDate(Instant.now().plus(3, ChronoUnit.HOURS))
                .category(category)
                .initiator(initiator)
                .paid(false)
                .state(EventState.PENDING)
                .participantLimit(10)
                .requestModeration(true)
                .build());

        User newRequester = userRepository.save(User.builder()
                .name("Another Requester")
                .email("another@test.com")
                .build());

        mockMvc.perform(post("/users/{userId}/requests", newRequester.getId())
                        .param("eventId", unpublishedEvent.getId().toString()))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldAddParticipationRequestWithAutoConfirm() throws Exception {
        Event autoConfirmEvent = eventRepository.save(Event.builder()
                .title("Auto Confirm Event")
                .annotation("This is auto confirm annotation that is long enough to pass validation")
                .description("This is auto confirm description that is long enough to pass validation")
                .lat(55.75)
                .lon(37.62)
                .eventDate(Instant.now().plus(3, ChronoUnit.HOURS))
                .category(category)
                .initiator(initiator)
                .paid(false)
                .state(EventState.PUBLISHED)
                .participantLimit(0)
                .requestModeration(false)
                .build());

        User newRequester = userRepository.save(User.builder()
                .name("Auto Requester")
                .email("auto@test.com")
                .build());

        mockMvc.perform(post("/users/{userId}/requests", newRequester.getId())
                        .param("eventId", autoConfirmEvent.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    public void shouldCancelParticipationRequest() throws Exception {
        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", requester.getId(), request.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(request.getId().intValue())))
                .andExpect(jsonPath("$.status", is("CANCELED")));
    }

    @Test
    public void shouldReturnConflictWhenCancelingOthersRequest() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .name("Other User")
                .email("other@test.com")
                .build());

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", otherUser.getId(), request.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldReturnNotFoundWhenCancelingNonExistingRequest() throws Exception {
        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", requester.getId(), 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWithNegativeUserId() throws Exception {
        mockMvc.perform(get("/users/{userId}/requests", -1))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWithNegativeEventId() throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", requester.getId())
                        .param("eventId", "-1"))
                .andExpect(status().isBadRequest());
    }
}
