package ru.practicum.ewm.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.dto.Formatter;
import ru.practicum.ewm.model.event.*;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class}, imports = Formatter.class)
public interface EventMapper {

    @Mapping(source = "lat", target = "location.lat")
    @Mapping(source = "lon", target = "location.lon")
    @Mapping(target = "eventDate", expression = "java(Formatter.format(event.getEventDate()))")
    @Mapping(target = "createdOn", expression = "java(Formatter.format(event.getCreatedOn()))")
    @Mapping(target = "publishedOn", expression = "java(event.getPublishedOn() == null ? null : Formatter.format(event.getPublishedOn()))")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventFullDto toFullDto(Event event);

    @Mapping(target = "eventDate", expression = "java(Formatter.format(event.getEventDate()))")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventShortDto toShortDto(Event event);

    @Mapping(source = "location.lat", target = "lat")
    @Mapping(source = "location.lon", target = "lon")
    @Mapping(target = "eventDate", expression = "java(Formatter.toInstant(dto.getEventDate()))")
    // ignore = true - я знаю что это поле не маппится, не ругайся пожалуйста компилятор
    @IgnoreEventMetadata
    Event toEvent(EventDtoRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "location.lat", target = "lat")
    @Mapping(source = "location.lon", target = "lon")
    @Mapping(target = "eventDate", expression = "java(dto.getEventDate() == null ? event.getEventDate() : Formatter.toInstant(dto.getEventDate()))")
    @IgnoreEventMetadata
    void merge(@MappingTarget Event event, EventDtoRequest dto);
}