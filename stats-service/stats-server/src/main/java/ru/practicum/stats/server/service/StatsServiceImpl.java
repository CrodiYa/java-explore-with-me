package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.exceptions.BadRequestException;
import ru.practicum.stats.server.mapper.DtoMapper;
import ru.practicum.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        if (start.isAfter(end))
            throw new BadRequestException("Неправильно заданное время");
        if (uris != null && !uris.isEmpty()) {
            if (unique)
                return repository.getStatsByUriWithUniqueIp(start, end, uris);
            return repository.getStatsByUri(start, end, uris);
        } else {
            if (unique)
                return repository.getStatsWithUniqueIp(start, end);
            return repository.getStats(start, end);
        }
    }

    @Override
    @Transactional
    public void saveHit(EndpointHitDto dto) {
        repository.save(DtoMapper.toEndpoint(dto));
    }
}
