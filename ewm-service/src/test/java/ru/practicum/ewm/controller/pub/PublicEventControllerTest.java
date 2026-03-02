package ru.practicum.ewm.controller.pub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.Formatter;
import ru.practicum.ewm.exception.GlobalExceptionHandler;
import ru.practicum.ewm.model.event.EventFullDto;
import ru.practicum.ewm.model.event.EventShortDto;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.event.Location;
import ru.practicum.ewm.model.category.CategoryDto;
import ru.practicum.ewm.model.user.UserDto;
import ru.practicum.ewm.service.event.EventService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicEventController.class)
@ContextConfiguration(classes = {PublicEventController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private EventShortDto eventShortDto;
    private EventFullDto eventFullDto;
    private final Long eventId = 1L;
    private final String baseUrl = "/events";

    @BeforeEach
    public void setUp() {
        String date = Formatter.format(Instant.now().plus(3, ChronoUnit.HOURS));
        eventShortDto = EventShortDto.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test Annotation")
                .category(new CategoryDto(1L, "Test Category"))
                .initiator(new UserDto(1L, "Test User", "test@email.com"))
                .eventDate(date)
                .paid(false)
                .build();

        eventFullDto = EventFullDto.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test Annotation")
                .description("Test Description")
                .category(new CategoryDto(1L, "Test Category"))
                .initiator(new UserDto(1L, "Test User", "test@email.com"))
                .eventDate(date)
                .location(new Location(55.75, 37.62))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .build();
    }

    @Nested
    class FindingPublicEvents {

        @Test
        public void shouldReturnEventsList() throws Exception {
            List<EventShortDto> events = List.of(eventShortDto);
            when(eventService.findPublicEvents(anyString(), anyList(), anyBoolean(), any(), any(), anyBoolean(),
                    anyString(), anyInt(), anyInt(), anyString()))
                    .thenReturn(events);

            mockMvc.perform(get(baseUrl)
                            .param("text", "concert")
                            .param("categories", "1", "2")
                            .param("paid", "true")
                            .param("rangeStart", "2023-01-01T00:00:00Z")
                            .param("rangeEnd", "2024-01-01T00:00:00Z")
                            .param("onlyAvailable", "true")
                            .param("sort", "EVENT_DATE")
                            .param("from", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].title", is("Test Event")));
        }

        @Test
        public void shouldUseDefaultPaginationWhenNotProvided() throws Exception {
            when(eventService.findPublicEvents(isNull(), isNull(), isNull(), isNull(), isNull(),
                    eq(false), isNull(), eq(0), eq(10), anyString()))
                    .thenReturn(List.of());

            mockMvc.perform(get(baseUrl))
                    .andExpect(status().isOk());
        }

        @Test
        public void shouldAcceptMinimalParameters() throws Exception {
            when(eventService.findPublicEvents(isNull(), isNull(), isNull(), isNull(), isNull(),
                    eq(false), isNull(), eq(0), eq(10), anyString()))
                    .thenReturn(List.of());

            mockMvc.perform(get(baseUrl))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class FindingPublicEvent {

        @Test
        public void shouldReturnEvent() throws Exception {
            when(eventService.findPublicEvent(eq(eventId), anyString()))
                    .thenReturn(eventFullDto);

            mockMvc.perform(get(baseUrl + "/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.title", is("Test Event")))
                    .andExpect(jsonPath("$.state", is("PUBLISHED")));
        }

        @Test
        public void shouldReturnBadRequestWhenIdNotPositive() throws Exception {
            mockMvc.perform(get(baseUrl + "/{id}", 0))
                    .andExpect(status().isBadRequest());
        }
    }
}
