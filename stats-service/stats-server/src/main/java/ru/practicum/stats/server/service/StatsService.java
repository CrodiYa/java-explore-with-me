package ru.practicum.stats.server.service;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                List<String> uris, boolean unique);

    EndpointHitDto saveHit(EndpointHitDto dto);
}
