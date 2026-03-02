package ru.practicum.ewm.controller.priv;

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
import ru.practicum.ewm.exception.GlobalExceptionHandler;
import ru.practicum.ewm.model.participation.ParticipationRequestDto;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.service.participation.ParticipationRequestService;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PrivateParticipationRequestController.class)
@ContextConfiguration(classes = {PrivateParticipationRequestController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class PrivateParticipationRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParticipationRequestService service;

    private ParticipationRequestDto requestDto;
    private final Long userId = 1L;
    private final Long requestId = 1L;
    private final Long eventId = 1L;
    private final String baseUrl = "/users/{userId}/requests";

    @BeforeEach
    public void setUp() {
        requestDto = ParticipationRequestDto.builder()
                .id(requestId)
                .requester(userId)
                .event(eventId)
                .created(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                .status(ParticipationStatus.PENDING)
                .build();
    }

    @Nested
    class FindingByRequesterId {

        @Test
        public void shouldReturnRequestsList() throws Exception {
            List<ParticipationRequestDto> requests = List.of(requestDto);
            when(service.findByRequesterId(userId)).thenReturn(requests);

            mockMvc.perform(get(baseUrl, userId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].status", is("PENDING")));
        }

        @Test
        public void shouldReturnEmptyListWhenNoRequests() throws Exception {
            when(service.findByRequesterId(userId)).thenReturn(List.of());

            mockMvc.perform(get(baseUrl, userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class AddingParticipationRequest {

        @Test
        public void shouldCreateRequestSuccessfully() throws Exception {
            when(service.addParticipationRequest(eq(userId), eq(eventId)))
                    .thenReturn(requestDto);

            mockMvc.perform(post(baseUrl, userId)
                            .param("eventId", eventId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.status", is("PENDING")));
        }

        @Test
        public void shouldReturnBadRequestWhenEventIdNotProvided() throws Exception {
            mockMvc.perform(post(baseUrl, userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnBadRequestWhenEventIdInvalid() throws Exception {
            mockMvc.perform(post(baseUrl, userId)
                            .param("eventId", "invalid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class CancelingParticipationRequest {

        @Test
        public void shouldCancelRequestSuccessfully() throws Exception {
            ParticipationRequestDto canceledDto = ParticipationRequestDto.builder()
                    .id(requestId)
                    .requester(userId)
                    .event(eventId)
                    .status(ParticipationStatus.CANCELED)
                    .build();

            when(service.cancelParticipationRequest(userId, requestId))
                    .thenReturn(canceledDto);

            mockMvc.perform(patch(baseUrl + "/{requestId}/cancel", userId, requestId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.status", is("CANCELED")));
        }

        @Test
        public void shouldReturnBadRequestWhenRequestIdNotPositive() throws Exception {
            mockMvc.perform(patch(baseUrl + "/{requestId}/cancel", userId, 0))
                    .andExpect(status().isBadRequest());
        }
    }
}
