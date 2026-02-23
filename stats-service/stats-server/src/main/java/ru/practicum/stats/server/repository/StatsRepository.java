package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {
    // возвращаем стату по уникальному ip
    @Query("select new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, count(distinct(hit.ip))) " +
            "from EndpointHit as hit " +
            "where hit.createDate >= :start AND hit.createDate <= :end AND hit.uri IN :uris " +
            "group by hit.app, hit.uri")
    List<ViewStatsDto> getStatsByUriWithUniqueIp(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris);

    @Query("select new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, count(distinct(hit.ip))) " +
            "from EndpointHit as hit " +
            "where hit.createDate >= :start AND hit.createDate <= :end " +
            "group by hit.app, hit.uri")
    List<ViewStatsDto> getStatsWithUniqueIp(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    @Query("select new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, count(hit)) " +
            "from EndpointHit as hit " +
            "where hit.createDate >= :start AND hit.createDate <= :end AND hit.uri IN :uris " +
            "group by hit.app, hit.uri")
    List<ViewStatsDto> getStatsByUri(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris);

    // возвращаем стату с начала до конца какого то времени
    @Query("select new ru.practicum.dto.ViewStatsDto(hit.app, hit.uri, count(hit)) " +
            "from EndpointHit as hit " +
            "where hit.createDate >= :start AND hit.createDate <= :end " +
            "group by hit.app, hit.uri")
    List<ViewStatsDto> getStats(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}
