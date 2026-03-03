package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.Instant;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {
    // возвращаем статистику по уникальному ip и конкретным uri
    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, COUNT(DISTINCT hit.ip))
            FROM EndpointHit as hit
            WHERE hit.createDate BETWEEN :start AND :end AND hit.uri IN :uris
            GROUP BY hit.app, hit.uri
            ORDER BY COUNT(DISTINCT hit.ip) desc
            """)
    List<ViewStatsDto> getStatsByUriWithUniqueIp(@Param("start") Instant start,
                                                 @Param("end") Instant end,
                                                 @Param("uris") List<String> uris);

    // возвращаем статистику по уникальному ip
    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, COUNT(DISTINCT hit.ip))
            FROM EndpointHit as hit
            WHERE hit.createDate BETWEEN :start AND :end
            GROUP BY hit.app, hit.uri
            ORDER BY COUNT(DISTINCT hit.ip) desc
            """)
    List<ViewStatsDto> getStatsWithUniqueIp(@Param("start") Instant start,
                                            @Param("end") Instant end);

    // возвращаем статистику по конкретным uri
    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, COUNT(hit))
            FROM EndpointHit as hit
            WHERE hit.createDate BETWEEN :start AND :end AND hit.uri IN :uris
            GROUP BY hit.app, hit.uri
            ORDER BY COUNT(hit) desc
            """)
    List<ViewStatsDto> getStatsByUri(@Param("start") Instant start,
                                     @Param("end") Instant end,
                                     @Param("uris") List<String> uris);

    // возвращаем статистику с начала до конца какого то времени
    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, COUNT(hit))
            FROM EndpointHit as hit
            WHERE hit.createDate BETWEEN :start AND :end
            GROUP BY hit.app, hit.uri
            ORDER BY COUNT(hit) desc
            """)
    List<ViewStatsDto> getStats(@Param("start") Instant start,
                                @Param("end") Instant end);
}