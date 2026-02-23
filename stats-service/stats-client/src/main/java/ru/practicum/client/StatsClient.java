package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.StatsRequest;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.util.List;

/**
 * Client for interacting with the statistics service.
 * Provides methods to record hits and retrieve view statistics.
 */
@Component
@Slf4j
public class StatsClient {

    @Value("${stats-service.url}")
    private String serverUrl;

    private final RestTemplate rest;

    public StatsClient(RestTemplateBuilder builder) {
        this.rest = builder.build();
    }

    /**
     * Sends a request to record a hit to the stats service.
     *
     * @param endpointHitDto the data of the hit to be recorded
     */
    public void hit(EndpointHitDto endpointHitDto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(endpointHitDto, headers);

            rest.exchange(
                    serverUrl + "/hit",
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Error during recording endpointHit: {}", endpointHitDto, e);
        }
    }

    /**
     * Retrieves view statistics from the stats service.
     *
     * @param statsRequest the request containing start/end dates, URIs, and uniqueness flag
     * @return a list of {@link ViewStatsDto} or {@code null} if any exception happens
     */
    public List<ViewStatsDto> getStats(StatsRequest statsRequest) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(serverUrl + "/stats")
                    .queryParam("start", statsRequest.getStart())
                    .queryParam("end", statsRequest.getEnd())
                    .queryParam("unique", statsRequest.getUnique());

            List<String> uris = statsRequest.getUris();

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    builder.queryParam("uris", uri);
                }
            }

            URI uri = builder.encode().build().toUri();

            return rest.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {
                    }
            ).getBody();

        } catch (Exception e) {
            log.error("Error during recording statsRequest: {}", statsRequest, e);
            return null;
        }
    }
}