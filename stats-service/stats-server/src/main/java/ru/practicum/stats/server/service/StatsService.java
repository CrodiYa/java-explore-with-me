package ru.practicum.stats.server.service;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.Instant;
import java.util.List;

public interface StatsService {
    List<ViewStatsDto> getStats(Instant start, Instant end,
                                List<String> uris, boolean unique);

    EndpointHitDto saveHit(EndpointHitDto dto);
}
