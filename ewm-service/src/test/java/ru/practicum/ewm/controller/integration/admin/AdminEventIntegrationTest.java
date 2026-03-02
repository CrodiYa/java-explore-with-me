package ru.practicum.ewm.controller.integration.admin;

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
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventDtoRequest;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.event.EventStateAction;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
public class AdminEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User initiator;
    private Category category;
    private Event event1;
    private Event event2;
    private Event event3;

    @BeforeEach
    public void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        initiator = userRepository.save(User.builder()
                .name("Initiator")
                .email("initiator@test.com")
                .build());

        User otherUser = userRepository.save(User.builder()
                .name("Other User")
                .email("other@test.com")
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Test Category")
                .build());

        Category otherCategory = categoryRepository.save(Category.builder()
                .name("Other Category")
                .build());

        Instant futureDate = Instant.now().plus(3, ChronoUnit.HOURS);

        event1 = eventRepository.save(Event.builder()
                .title("Event 1")
                .annotation("Annotation for event 1 that is long enough")
                .description("Description for event 1 that is long enough")
                .lat(55.75)
                .lon(37.62)
                .eventDate(futureDate)
                .category(category)
                .initiator(initiator)
                .paid(false)
                .state(EventState.PENDING)
                .participantLimit(10)
                .requestModeration(true)
                .build());

        event2 = eventRepository.save(Event.builder()
                .title("Event 2")
                .annotation("Annotation for event 2 that is long enough")
                .description("Description for event 2 that is long enough")
                .lat(55.76)
                .lon(37.63)
                .eventDate(futureDate)
                .category(category)
                .initiator(initiator)
                .paid(true)
                .state(EventState.PUBLISHED)
                .participantLimit(5)
                .requestModeration(false)
                .build());

        event3 = eventRepository.save(Event.builder()
                .title("Event 3")
                .annotation("Annotation for event 3 that is long enough")
                .description("Description for event 3 that is long enough")
                .lat(55.77)
                .lon(37.64)
                .eventDate(futureDate)
                .category(otherCategory)
                .initiator(otherUser)
                .paid(false)
                .state(EventState.CANCELED)
                .participantLimit(0)
                .requestModeration(true)
                .build());
    }

    @Test
    public void shouldFindAllEventsWithoutFilters() throws Exception {
        mockMvc.perform(get("/admin/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(event1.getId().intValue())))
                .andExpect(jsonPath("$[1].id", is(event2.getId().intValue())))
                .andExpect(jsonPath("$[2].id", is(event3.getId().intValue())));
    }

    @Test
    public void shouldFindEventsByUsers() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("users", initiator.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].initiator.id", is(initiator.getId().intValue())))
                .andExpect(jsonPath("$[1].initiator.id", is(initiator.getId().intValue())));
    }

    @Test
    public void shouldFindEventsByStates() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("states", "PENDING", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].state", is("PENDING")))
                .andExpect(jsonPath("$[1].state", is("PUBLISHED")));
    }

    @Test
    public void shouldFindEventsByCategories() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("categories", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].category.id", is(category.getId().intValue())))
                .andExpect(jsonPath("$[1].category.id", is(category.getId().intValue())));
    }

    @Test
    public void shouldFindEventsByDateRange() throws Exception {
        String start = "2025-01-01 00:00:00";
        String end = "2027-01-01 00:00:00";

        mockMvc.perform(get("/admin/events")
                        .param("rangeStart", start)
                        .param("rangeEnd", end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    public void shouldReturnBadRequestWhenStartAfterEnd() throws Exception {
        String start = "2026-01-01 00:00:00";
        String end = "2025-01-01 00:00:00";

        mockMvc.perform(get("/admin/events")
                        .param("rangeStart", start)
                        .param("rangeEnd", end))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFindEventsWithPagination() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("from", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/admin/events")
                        .param("from", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void shouldPatchEventAsAdmin() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .title("Updated Title")
                .annotation("Updated annotation that is long enough for validation")
                .stateAction(EventStateAction.PUBLISH_EVENT)
                .build();

        mockMvc.perform(patch("/admin/events/{eventId}", event1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(event1.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.state", is("PUBLISHED")));
    }

    @Test
    public void shouldRejectEventAsAdmin() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .stateAction(EventStateAction.REJECT_EVENT)
                .build();

        mockMvc.perform(patch("/admin/events/{eventId}", event1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(event1.getId().intValue())))
                .andExpect(jsonPath("$.state", is("CANCELED")));
    }

    @Test
    public void shouldReturnConflictWhenPublishingNonPendingEvent() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .stateAction(EventStateAction.PUBLISH_EVENT)
                .build();

        mockMvc.perform(patch("/admin/events/{eventId}", event2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldReturnConflictWhenRejectingPublishedEvent() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .stateAction(EventStateAction.REJECT_EVENT)
                .build();

        mockMvc.perform(patch("/admin/events/{eventId}", event2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldUpdateEventWithNewCategory() throws Exception {
        Category newCategory = categoryRepository.save(Category.builder()
                .name("New Category")
                .build());

        EventDtoRequest request = EventDtoRequest.builder()
                .category(newCategory.getId())
                .build();

        mockMvc.perform(patch("/admin/events/{eventId}", event1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id", is(newCategory.getId().intValue())));
    }

    @Test
    public void shouldReturnNotFoundWhenPatchingNonExistingEvent() throws Exception {
        EventDtoRequest request = EventDtoRequest.builder()
                .title("Updated Title")
                .build();

        mockMvc.perform(patch("/admin/events/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
