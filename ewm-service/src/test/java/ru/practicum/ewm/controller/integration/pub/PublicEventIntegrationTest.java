package ru.practicum.ewm.controller.integration.pub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.Formatter;
import ru.practicum.dto.ViewStatsDto;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
public class PublicEventIntegrationTest {

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

    @Autowired
    private ParticipationRequestRepository requestRepository;

    @MockBean
    private StatsClient statsClient;

    private User initiator;
    private Category category1;
    private Category category2;
    private Event event1;
    private Event event2;
    private Event event3;
    private Event event4;
    private Event limitedEvent;

    @BeforeEach
    public void setUp() {
        requestRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        initiator = userRepository.save(User.builder()
                .name("Initiator")
                .email("initiator@test.com")
                .build());

        category1 = categoryRepository.save(Category.builder()
                .name("Category 1")
                .build());

        category2 = categoryRepository.save(Category.builder()
                .name("Category 2")
                .build());

        Instant now = Instant.now();
        Instant futureDate1 = now.plus(3, ChronoUnit.HOURS);
        Instant futureDate2 = now.plus(5, ChronoUnit.HOURS);
        Instant futureDate3 = now.plus(7, ChronoUnit.HOURS);

        event1 = eventRepository.save(Event.builder()
                .title("Java Conference")
                .annotation("Learn about Java programming language and best practices")
                .description("Comprehensive Java conference with workshops and talks")
                .lat(55.75)
                .lon(37.62)
                .eventDate(futureDate1)
                .category(category1)
                .initiator(initiator)
                .paid(false)
                .state(EventState.PUBLISHED)
                .participantLimit(10)
                .requestModeration(true)
                .build());

        event2 = eventRepository.save(Event.builder()
                .title("Python Workshop")
                .annotation("Hands-on Python programming workshop for beginners")
                .description("Learn Python from scratch with practical exercises")
                .lat(55.76)
                .lon(37.63)
                .eventDate(futureDate2)
                .category(category1)
                .initiator(initiator)
                .paid(true)
                .state(EventState.PUBLISHED)
                .participantLimit(5)
                .requestModeration(false)
                .build());

        event3 = eventRepository.save(Event.builder()
                .title("Music Festival")
                .annotation("Annual music festival with various artists")
                .description("Enjoy live music performances throughout the day")
                .lat(55.77)
                .lon(37.64)
                .eventDate(futureDate3)
                .category(category2)
                .initiator(initiator)
                .paid(true)
                .state(EventState.PUBLISHED)
                .participantLimit(0)
                .requestModeration(true)
                .build());

        event4 = eventRepository.save(Event.builder()
                .title("Unpublished Event")
                .annotation("This event should not appear in public search")
                .description("This event is not published yet")
                .lat(55.78)
                .lon(37.65)
                .eventDate(futureDate1)
                .category(category2)
                .initiator(initiator)
                .paid(false)
                .state(EventState.PENDING)
                .participantLimit(10)
                .requestModeration(true)
                .build());

        limitedEvent = eventRepository.save(Event.builder()
                .title("Limited Event")
                .annotation("This event has limited spots")
                .description("Only few spots available")
                .lat(55.75)
                .lon(37.62)
                .eventDate(futureDate1)
                .category(category1)
                .initiator(initiator)
                .paid(false)
                .state(EventState.PUBLISHED)
                .participantLimit(1)
                .requestModeration(true)
                .build());

        // Настройка моков для статистики
        ViewStatsDto viewStats1 = new ViewStatsDto();
        viewStats1.setUri("/events/" + event1.getId());
        viewStats1.setHits(15L);

        ViewStatsDto viewStats2 = new ViewStatsDto();
        viewStats2.setUri("/events/" + event2.getId());
        viewStats2.setHits(10L);

        ViewStatsDto viewStats3 = new ViewStatsDto();
        viewStats3.setUri("/events/" + event3.getId());
        viewStats3.setHits(5L);

        ViewStatsDto viewStatsLimited = new ViewStatsDto();
        viewStatsLimited.setUri("/events/" + limitedEvent.getId());
        viewStatsLimited.setHits(2L);

        when(statsClient.getStats(any()))
                .thenReturn(List.of(viewStats1, viewStats2, viewStats3, viewStatsLimited));

        doNothing().when(statsClient).hit(any(EndpointHitDto.class));
    }

    @Test
    public void shouldFindAllPublishedEvents() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
        verify(statsClient).hit(any(EndpointHitDto.class));
        verify(statsClient).getStats(any());
    }

    @Test
    public void shouldFindEventsByTextSearch() throws Exception {
        mockMvc.perform(get("/events")
                        .param("text", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(event1.getId().intValue())))
                .andExpect(jsonPath("$[0].title", containsString("Java")))
                .andExpect(jsonPath("$[0].views", is(15)));

        mockMvc.perform(get("/events")
                        .param("text", "Python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(event2.getId().intValue())))
                .andExpect(jsonPath("$[0].views", is(10)));

        mockMvc.perform(get("/events")
                        .param("text", "Music"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(event3.getId().intValue())))
                .andExpect(jsonPath("$[0].views", is(5)));
    }

    @Test
    public void shouldFindEventsByCategories() throws Exception {
        mockMvc.perform(get("/events")
                        .param("categories", category1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].category.id", is(category1.getId().intValue())))
                .andExpect(jsonPath("$[1].category.id", is(category1.getId().intValue())))
                .andExpect(jsonPath("$[2].category.id", is(category1.getId().intValue())));

        mockMvc.perform(get("/events")
                        .param("categories", category2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category.id", is(category2.getId().intValue())));
    }

    @Test
    public void shouldFindEventsByPaid() throws Exception {
        mockMvc.perform(get("/events")
                        .param("paid", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].paid", is(true)))
                .andExpect(jsonPath("$[1].paid", is(true)));

        mockMvc.perform(get("/events")
                        .param("paid", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].paid", is(false)))
                .andExpect(jsonPath("$[1].paid", is(false)));
    }

    @Test
    public void shouldFindEventsByDateRange() throws Exception {
        String start = Formatter.format(Instant.now().plus(2, ChronoUnit.HOURS));
        String end = Formatter.format(Instant.now().plus(6, ChronoUnit.HOURS));

        mockMvc.perform(get("/events")
                        .param("rangeStart", start)
                        .param("rangeEnd", end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is(event1.getId().intValue())))
                .andExpect(jsonPath("$[1].id", is(limitedEvent.getId().intValue())))
                .andExpect(jsonPath("$[2].id", is(event2.getId().intValue())));
    }

    @Test
    public void shouldReturnBadRequestWhenStartAfterEnd() throws Exception {
        String start = Formatter.format(Instant.now().plus(6, ChronoUnit.HOURS));
        String end = Formatter.format(Instant.now().plus(2, ChronoUnit.HOURS));

        mockMvc.perform(get("/events")
                        .param("rangeStart", start)
                        .param("rangeEnd", end))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFindOnlyAvailableEvents() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .name("Other User")
                .email("other@test.com")
                .build());

        requestRepository.save(ParticipationRequest.builder()
                .event(limitedEvent)
                .requester(otherUser)
                .status(ParticipationStatus.CONFIRMED)
                .build());

        mockMvc.perform(get("/events")
                        .param("onlyAvailable", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.title=='Limited Event')]").doesNotExist());
    }

    @Test
    public void shouldSortEventsByEventDate() throws Exception {
        mockMvc.perform(get("/events")
                        .param("sort", "EVENT_DATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id", is(event1.getId().intValue())))
                .andExpect(jsonPath("$[1].id", is(limitedEvent.getId().intValue())))
                .andExpect(jsonPath("$[2].id", is(event2.getId().intValue())))
                .andExpect(jsonPath("$[3].id", is(event3.getId().intValue())));
    }

    @Test
    public void shouldSortEventsByViews() throws Exception {
        mockMvc.perform(get("/events")
                        .param("sort", "VIEWS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].views", is(15)))
                .andExpect(jsonPath("$[1].views", is(10)))
                .andExpect(jsonPath("$[2].views", is(5)))
                .andExpect(jsonPath("$[3].views", is(2)));
    }

    @Test
    public void shouldFindEventsWithPagination() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/events")
                        .param("from", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void shouldFindPublicEventById() throws Exception {
        mockMvc.perform(get("/events/{id}", event1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(event1.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Java Conference")))
                .andExpect(jsonPath("$.state", is("PUBLISHED")))
                .andExpect(jsonPath("$.views", is(15)))
                .andExpect(jsonPath("$.confirmedRequests", notNullValue()));

        verify(statsClient).hit(any(EndpointHitDto.class));
        verify(statsClient).getStats(any());
    }

    @Test
    public void shouldReturnNotFoundWhenEventNotPublished() throws Exception {
        mockMvc.perform(get("/events/{id}", event4.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        mockMvc.perform(get("/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldIncludeConfirmedRequestsCount() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .name("Other User")
                .email("other@test.com")
                .build());

        User thirdUser = userRepository.save(User.builder()
                .name("Third User")
                .email("third@test.com")
                .build());

        requestRepository.save(ParticipationRequest.builder()
                .event(event1)
                .requester(otherUser)
                .status(ParticipationStatus.CONFIRMED)
                .build());

        requestRepository.save(ParticipationRequest.builder()
                .event(event1)
                .requester(thirdUser)
                .status(ParticipationStatus.CONFIRMED)
                .build());

        mockMvc.perform(get("/events/{id}", event1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", is(2)));
    }

    @Test
    public void shouldUseDefaultPaginationValues() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    public void shouldReturnBadRequestWithNegativeId() throws Exception {
        mockMvc.perform(get("/events/-1"))
                .andExpect(status().isBadRequest());
    }
}
