package ru.practicum.stats.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.Formatter;
import ru.practicum.stats.server.model.EndpointHit;

@Mapper(componentModel = "spring", imports = Formatter.class)
public interface DtoMapper {

    @Mapping(target = "createDate", expression = "java(Formatter.toInstant(dto.getTimestamp()))")
    EndpointHit toEndpoint(EndpointHitDto dto);

    @Mapping(target = "timestamp", expression = "java(Formatter.format(hit.getCreateDate()))")
    EndpointHitDto toEndpointDto(EndpointHit hit);
}
