package ru.practicum.stats.server.mapper;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.LocalDateTime;

import static ru.practicum.dto.Formatter.format;
import static ru.practicum.dto.Formatter.toLocalDateTime;

public class DtoMapper {

    public static EndpointHit toEndpoint(EndpointHitDto dto) {
        LocalDateTime createDate = toLocalDateTime(dto.getTimestamp());

        return EndpointHit.builder()
                .app(dto.getApp())
                .ip(dto.getIp())
                .uri(dto.getUri())
                .createDate(createDate)
                .build();
    }

    public static EndpointHitDto from(EndpointHit hit) {
        String timestamp = format(hit.getCreateDate());

        return EndpointHitDto.builder()
                .ip(hit.getIp())
                .app(hit.getApp())
                .uri(hit.getUri())
                .timestamp(timestamp)
                .build();
    }
}
