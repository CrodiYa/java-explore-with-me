package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {
    // возвращаем статистику по уникальному ip и конкретным uri
    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, COUNT(DISTINCT(hit.ip)) as count)
            FROM EndpointHit as hit
            WHERE hit.createDate BETWEEN :start AND :end AND hit.uri IN :uris
            GROUP BY hit.app, hit.uri
            ORDER BY count desc
            """)
    List<ViewStatsDto> getStatsByUriWithUniqueIp(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("uris") List<String> uris);

    // возвращаем статистику по уникальному ip
    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, COUNT(DISTINCT(hit.ip)) as count)
            FROM EndpointHit as hit
            WHERE hit.createDate BETWEEN :start AND :end
            GROUP BY hit.app, hit.uri
            ORDER BY count desc
            """)
    List<ViewStatsDto> getStatsWithUniqueIp(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    // возвращаем статистику по конкретным uri
    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, COUNT(hit) as count)
            FROM EndpointHit as hit
            WHERE hit.createDate BETWEEN :start AND :end AND hit.uri IN :uris
            GROUP BY hit.app, hit.uri
            ORDER BY count desc
            """)
    List<ViewStatsDto> getStatsByUri(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("uris") List<String> uris);

    // возвращаем статистику с начала до конца какого то времени
    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, COUNT(hit) as count)
            FROM EndpointHit as hit
            WHERE hit.createDate BETWEEN :start AND :end
            GROUP BY hit.app, hit.uri
            ORDER BY count desc
            """)
    List<ViewStatsDto> getStats(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}