package ru.practicum.stats.server.repository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class StatsRepositoryTest {

    private final StatsRepository repository;

    private final Instant start = Instant.parse("2000-01-01T00:00:00Z");
    private final Instant end = start.plus(1, ChronoUnit.DAYS);
    private final String app = "app";
    private final String ip = "ip";
    private final String uri = "uri";
    private final Long notUniqueHits = 4L;
    private final Long uniqueHits = 3L;
    private final String anotherUri = "anotherUri";
    private final String anotherIp = "anotherIp";

    @BeforeEach
    public void setUp() {
        EndpointHit hit = EndpointHit.builder()
                .ip(ip)
                .uri(uri)
                .app(app)
                .createDate(start)
                .build();

        EndpointHit hit3 = EndpointHit.builder()
                .ip(anotherIp)
                .uri(uri)
                .app(app)
                .createDate(start)
                .build();

        EndpointHit hit4 = EndpointHit.builder()
                .ip(anotherIp + "1")
                .uri(uri)
                .app(app)
                .createDate(start)
                .build();

        EndpointHit hit5 = EndpointHit.builder()
                .ip(ip)
                .uri(anotherUri)
                .app(app)
                .createDate(start)
                .build();

        EndpointHit hit2 = EndpointHit.builder()
                .ip(ip)
                .uri(uri)
                .app(app)
                .createDate(start)
                .build();


        repository.save(hit);
        repository.save(hit2);
        repository.save(hit3);
        repository.save(hit4);
        repository.save(hit5);
    }

    @Test
    public void shouldGetStats() {
        List<ViewStatsDto> all = repository.getStats(start, end);
        assertThat(all).hasSize(2);
        assertEquals(uri, all.getFirst().getUri());
        assertEquals(anotherUri, all.getLast().getUri());

        assertEquals(notUniqueHits, all.getFirst().getHits());
        assertEquals(1, all.getLast().getHits());
    }

    @Test
    public void shouldGetStatsWithUniqueIp() {
        List<ViewStatsDto> unique = repository.getStatsWithUniqueIp(start, end);

        assertThat(unique).hasSize(2);
        assertEquals(uri, unique.getFirst().getUri());
        assertEquals(anotherUri, unique.getLast().getUri());

        assertEquals(uniqueHits, unique.getFirst().getHits());
        assertEquals(1, unique.getLast().getHits());
    }

    @Test
    public void shouldGetStatsByUri() {
        List<ViewStatsDto> uriNotUnique = repository.getStatsByUri(start, end, List.of(uri));

        assertThat(uriNotUnique).hasSize(1);
        assertEquals(uri, uriNotUnique.getFirst().getUri());
        assertEquals(notUniqueHits, uriNotUnique.getFirst().getHits());
    }

    @Test
    public void shouldGetStatsByUriWithUniqueIp() {
        List<ViewStatsDto> uriUnique = repository.getStatsByUriWithUniqueIp(start, end, List.of(uri));
        assertThat(uriUnique).hasSize(1);
        assertEquals(uri, uriUnique.getFirst().getUri());
        assertEquals(uniqueHits, uriUnique.getFirst().getHits());
    }
}