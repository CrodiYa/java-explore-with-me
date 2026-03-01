package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.exceptions.BadRequestException;
import ru.practicum.stats.server.mapper.DtoMapper;
import ru.practicum.stats.server.repository.StatsRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;
    // теперь это тоже spring-bean который надо инжектить
    private final DtoMapper dtoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(Instant start, Instant end,
                                       List<String> uris, boolean unique) {
        if (start.isAfter(end) || start.equals(end)) {
            throw new BadRequestException("Неправильно заданное время");
        }

        if (uris != null && !uris.isEmpty()) {
            return unique
                    ? repository.getStatsByUriWithUniqueIp(start, end, uris)
                    : repository.getStatsByUri(start, end, uris);
        } else {
            return unique
                    ? repository.getStatsWithUniqueIp(start, end)
                    : repository.getStats(start, end);
        }
    }

    @Override
    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto dto) {
        // dtoMapper.toEndpoint(dto) - т к репо принимает entity
        return dtoMapper.toEndpointDto(repository.save(dtoMapper.toEndpoint(dto)));
    }
}
