package ru.practicum.stats.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.Formatter;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.exceptions.BadRequestException;
import ru.practicum.stats.server.handler.ErrorHandler;
import ru.practicum.stats.server.service.StatsService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {StatsController.class, ErrorHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsService statsService;

    @Test
    void shouldSaveHit() throws Exception {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("192.168.0.1")
                .timestamp(Formatter.format(Instant.now()))
                .build();

        when(statsService.saveHit(any(EndpointHitDto.class))).thenReturn(hitDto);

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.app").value("test-app"))
                .andExpect(jsonPath("$.uri").value("/test"))
                .andExpect(jsonPath("$.ip").value("192.168.0.1"));
    }

    @Test
    void shouldGetStatsWithoutUris() throws Exception {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = Instant.now();
        List<ViewStatsDto> stats = List.of(
                ViewStatsDto.builder()
                        .app("test-app")
                        .uri("/test")
                        .hits(5L)
                        .build()
        );

        when(statsService.getStats(any(Instant.class), any(Instant.class),
                isNull(), anyBoolean())).thenReturn(stats);

        mockMvc.perform(get("/stats")
                        .param("start", Formatter.format(start))
                        .param("end", Formatter.format(end))
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("test-app"))
                .andExpect(jsonPath("$[0].uri").value("/test"))
                .andExpect(jsonPath("$[0].hits").value(5));
    }

    @Test
    void shouldGetStatsWithUris() throws Exception {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = Instant.now();

        List<ViewStatsDto> stats = List.of(
                ViewStatsDto.builder()
                        .app("test-app")
                        .uri("/test1")
                        .hits(3L)
                        .build(),
                ViewStatsDto.builder()
                        .app("test-app")
                        .uri("/test2")
                        .hits(7L)
                        .build()
        );

        when(statsService.getStats(any(Instant.class), any(Instant.class),
                anyList(), anyBoolean())).thenReturn(stats);

        mockMvc.perform(get("/stats")
                        .param("start", Formatter.format(start))
                        .param("end", Formatter.format(end))
                        .param("uris", "/test1", "/test2")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uri").value("/test1"))
                .andExpect(jsonPath("$[0].hits").value(3))
                .andExpect(jsonPath("$[1].uri").value("/test2"))
                .andExpect(jsonPath("$[1].hits").value(7));
    }

    @Test
    void shouldGetStatsWithUniqueIp() throws Exception {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = Instant.now();
        List<ViewStatsDto> stats = List.of(
                ViewStatsDto.builder()
                        .app("test-app")
                        .uri("/test")
                        .hits(2L)
                        .build()
        );

        when(statsService.getStats(any(Instant.class), any(Instant.class),
                isNull(), eq(true))).thenReturn(stats);

        mockMvc.perform(get("/stats")
                        .param("start", Formatter.format(start))
                        .param("end", Formatter.format(end))
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(2));
    }

    @Test
    void shouldGetStatsWithDefaultUniqueValue() throws Exception {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = Instant.now();
        List<ViewStatsDto> stats = List.of();

        when(statsService.getStats(any(Instant.class), any(Instant.class),
                isNull(), eq(false))).thenReturn(stats);

        mockMvc.perform(get("/stats")
                        .param("start", Formatter.format(start))
                        .param("end", Formatter.format(end)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestWhenStartDateAfterEndDate() throws Exception {
        Instant start = Instant.now();
        Instant end = Instant.now().minus(1, ChronoUnit.DAYS);

        when(statsService.getStats(any(Instant.class), any(Instant.class),
                isNull(), eq(false))).thenThrow(BadRequestException.class);

        mockMvc.perform(get("/stats")
                        .param("start", Formatter.format(start))
                        .param("end", Formatter.format(end)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenStartDateIsMissing() throws Exception {
        Instant end = Instant.now();

        mockMvc.perform(get("/stats")
                        .param("end", Formatter.format(end)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsMissing() throws Exception {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);

        mockMvc.perform(get("/stats")
                        .param("start", Formatter.format(start)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2024-01-01")
                        .param("end", "2024-01-02"))
                .andExpect(status().isBadRequest());
    }
}