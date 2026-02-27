package ru.practicum.stats.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.RandomHelper;
import ru.practicum.stats.server.exceptions.BadRequestException;
import ru.practicum.stats.server.mapper.DtoMapper;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.repository.StatsRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StatsServiceImplTest {

    @Mock
    private StatsRepository repository;

    @Mock
    private DtoMapper dtoMapper;

    @InjectMocks
    private StatsServiceImpl statsService;

    @Test
    public void shouldSave() {
        EndpointHitDto dtoHit = RandomHelper.getEndpointHitDto();

        EndpointHit savedHit = EndpointHit.builder()
                .app(dtoHit.getApp())
                .uri(dtoHit.getUri())
                .ip(dtoHit.getIp())
                .createDate(Instant.now())
                .build();

        when(dtoMapper.toEndpoint(dtoHit)).thenReturn(savedHit);
        // когда сервис вызовет репо.save(любой EndpointHit) - верни savedHit
        when(repository.save(any(EndpointHit.class))).thenReturn(savedHit);
        when(dtoMapper.toEndpointDto(savedHit)).thenReturn(dtoHit);

        EndpointHitDto savedDto = statsService.saveHit(dtoHit);

        assertEquals(dtoHit.getApp(), savedDto.getApp());
        assertEquals(dtoHit.getUri(), savedDto.getUri());
        assertEquals(dtoHit.getIp(), savedDto.getIp());
        assertEquals(dtoHit.getTimestamp(), savedDto.getTimestamp());

        verify(repository).save(any(EndpointHit.class));
    }

    @Test
    public void shouldGetStatsNoUriNotUnique() {
        Instant start = Instant.now();
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);

        List<ViewStatsDto> valid = List.of(
                RandomHelper.getViewStatsDto(),
                RandomHelper.getViewStatsDto()
        );

        when(repository.getStats(start, end)).thenReturn(valid);

        List<ViewStatsDto> list = statsService.getStats(start, end, null, false);

        assertEquals(valid, list);

        verify(repository).getStats(start, end);

        verify(repository, never()).getStatsWithUniqueIp(any(), any());
        verify(repository, never()).getStatsByUri(any(), any(), anyList());
        verify(repository, never()).getStatsByUriWithUniqueIp(any(), any(), anyList());
    }

    @Test
    public void shouldGetStatsNoUriUnique() {
        Instant start = Instant.now();
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);

        List<ViewStatsDto> valid = List.of(
                RandomHelper.getViewStatsDto(),
                RandomHelper.getViewStatsDto()
        );

        when(repository.getStatsWithUniqueIp(start, end)).thenReturn(valid);

        List<ViewStatsDto> list = statsService.getStats(start, end, Collections.emptyList(), true);

        assertEquals(valid, list);

        verify(repository).getStatsWithUniqueIp(start, end);

        verify(repository, never()).getStats(any(), any());
        verify(repository, never()).getStatsByUri(any(), any(), anyList());
        verify(repository, never()).getStatsByUriWithUniqueIp(any(), any(), anyList());
    }

    @Test
    public void shouldGetStatsUriNotUnique() {
        Instant start = Instant.now();
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);
        List<String> uris = List.of("/uri", "/uri1");

        List<ViewStatsDto> valid = List.of(new ViewStatsDto("app", "/uri", 3L),
                new ViewStatsDto("app1", "/uri1", 2L));

        when(repository.getStatsByUri(start, end, uris)).thenReturn(valid);

        List<ViewStatsDto> list = statsService.getStats(start, end, uris, false);

        assertEquals(valid, list);

        verify(repository).getStatsByUri(start, end, uris);

        verify(repository, never()).getStats(any(), any());
        verify(repository, never()).getStatsWithUniqueIp(any(), any());
        verify(repository, never()).getStatsByUriWithUniqueIp(any(), any(), anyList());
    }

    @Test
    public void shouldGetStatsUriUnique() {
        Instant start = Instant.now();
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);
        List<String> uris = List.of("/uri", "/uri1");

        List<ViewStatsDto> valid = List.of(new ViewStatsDto("app", "/uri", 3L),
                new ViewStatsDto("app1", "/uri1", 2L));

        when(repository.getStatsByUriWithUniqueIp(start, end, uris)).thenReturn(valid);

        List<ViewStatsDto> list = statsService.getStats(start, end, uris, true);

        assertEquals(valid, list);

        verify(repository).getStatsByUriWithUniqueIp(start, end, uris);

        verify(repository, never()).getStats(any(), any());
        verify(repository, never()).getStatsWithUniqueIp(any(), any());
        verify(repository, never()).getStatsByUri(any(), any(), anyList());
    }

    @Test
    public void shouldThrowWhenStartAfterEnd() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = Instant.now();
        assertThrows(BadRequestException.class, () -> statsService.getStats(start, end, null, false));
    }

    @Test
    public void shouldThrowWhenStartEqualEnd() {
        Instant start = Instant.now();
        Instant end = start;
        assertThrows(BadRequestException.class, () -> statsService.getStats(start, end, null, false));
    }

}