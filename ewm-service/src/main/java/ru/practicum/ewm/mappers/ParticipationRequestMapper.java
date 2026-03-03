package ru.practicum.ewm.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.Formatter;
import ru.practicum.ewm.model.participation.ParticipationRequest;
import ru.practicum.ewm.model.participation.ParticipationRequestDto;

@Mapper(componentModel = "spring", imports = Formatter.class)
public interface ParticipationRequestMapper {

    // source - принимаем (ParticipationRequest)
    // target - возвращаем (ParticipationRequestDto)
    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "requester.id", target = "requester")
    @Mapping(target = "created", expression = "java(Formatter.format(participationRequest.getCreated()))")
    ParticipationRequestDto toDto(ParticipationRequest participationRequest);
}
