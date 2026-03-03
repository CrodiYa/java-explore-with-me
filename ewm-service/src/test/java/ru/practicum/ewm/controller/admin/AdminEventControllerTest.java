package ru.practicum.ewm.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import ru.practicum.ewm.model.category.CategoryDto;
import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.model.user.UserDto;
import ru.practicum.ewm.service.event.EventService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminEventController.class)
@ContextConfiguration(classes = {AdminEventController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private EventFullDto eventFullDto;
    private EventDtoRequest updateRequest;
    private final Long eventId = 1L;
    private final String baseUrl = "/admin/events";

    @BeforeEach
    public void setUp() {
        eventFullDto = EventFullDto.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test Annotation")
                .description("Test Description")
                .category(new CategoryDto(1L, "Test Category"))
                .initiator(new UserDto(1L, "Test User", "test@email.com"))
                .eventDate(Formatter.format(Instant.now().plus(3, ChronoUnit.HOURS)))
                .location(new Location(55.75, 37.62))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(EventState.PENDING)
                .build();

        updateRequest = EventDtoRequest.builder()
                .title("Updated Event")
                .annotation("Updated Annotation")
                .description("Updated Description")
                .eventDate(Instant.now().plus(5, ChronoUnit.HOURS).toString())
                .location(new Location(55.75, 37.62))
                .category(2L)
                .paid(true)
                .participantLimit(100)
                .requestModeration(false)
                .stateAction(EventStateAction.PUBLISH_EVENT)
                .build();
    }


    @Test
    public void shouldReturnEventsList() throws Exception {
        List<EventFullDto> events = List.of(eventFullDto);
        when(eventService.findAdminEvents(anyList(), anyList(), anyList(), any(), any(), anyInt(), anyInt()))
                .thenReturn(events);

        mockMvc.perform(get(baseUrl)
                        .param("users", "1", "2")
                        .param("states", "PENDING", "PUBLISHED")
                        .param("categories", "1", "2")
                        .param("rangeStart", "2020-01-01 00:00:00")
                        .param("rangeEnd", "2030-01-01 00:00:00")
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
        when(eventService.findAdminEvents(any(), any(), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get(baseUrl))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldAcceptMinimalParameters() throws Exception {
        when(eventService.findAdminEvents(isNull(), isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get(baseUrl))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldPatchEventSuccessfully() throws Exception {
        EventFullDto updatedDto = EventFullDto.builder()
                .id(eventId)
                .title("Updated Event")
                .annotation("Very Big Updated Annotation")
                .description("Very Big Updated Description")
                .state(EventState.PUBLISHED)
                .build();

        EventDtoRequest request = EventDtoRequest.builder()
                .title("Updated Event")
                .annotation("Very Big Updated Annotation")
                .description("Very Big Updated Description")
                .stateAction(EventStateAction.PUBLISH_EVENT)
                .build();

        when(eventService.patchAdminEvent(eventId, request))
                .thenReturn(updatedDto);

        mockMvc.perform(patch(baseUrl + "/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Updated Event")))
                .andExpect(jsonPath("$.state", is("PUBLISHED")));
    }

    @Test
    public void shouldReturnBadRequestWhenEventIdNotPositive() throws Exception {
        mockMvc.perform(patch(baseUrl + "/{eventId}", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenRequestBodyInvalid() throws Exception {
        EventDtoRequest invalidRequest = EventDtoRequest.builder()
                .title("a")
                .annotation("short")
                .description("short")
                .build();

        mockMvc.perform(patch(baseUrl + "/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
