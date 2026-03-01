package ru.practicum.stats.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.service.StatsService;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHitDto saveHit(@RequestBody @Valid EndpointHitDto endpointHitDto) {
        log.info("На uri: {} сервиса был отправлен запрос пользователем.", endpointHitDto.getUri());
        return statsService.saveHit(endpointHitDto);
    }

    // @DateTimeFormat — она не работает с Instant напрямую
    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(@RequestParam Instant start,
                                       @RequestParam Instant end,
                                       @RequestParam(required = false) List<String> uris,
                                       @RequestParam(defaultValue = "false") boolean unique) {
        log.info("Поступил запрос на получение статистики запросов c параметрами start: {}, end {}, uris {}, unique {}",
                start, end, uris, unique);
        return statsService.getStats(start, end, uris, unique);
    }
}
