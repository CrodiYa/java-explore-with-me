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
    EventFullDto toFullDto(Event event);

    @Mapping(target = "eventDate", expression = "java(Formatter.format(event.getEventDate()))")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventShortDto toShortDto(Event event);

    @Mapping(source = "location.lat", target = "lat")
    @Mapping(source = "location.lon", target = "lon")
    @Mapping(target = "eventDate", expression = "java(Formatter.toInstant(dto.getEventDate()))")
    // ignore = true - я знаю что это поле не маппится, не ругайся пожалуйста компилятор
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    Event toEvent(EventDtoRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "location.lat", target = "lat")
    @Mapping(source = "location.lon", target = "lon")
    @Mapping(target = "eventDate", expression = "java(dto.getEventDate() == null ? event.getEventDate() : Formatter.toInstant(dto.getEventDate()))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    void merge(@MappingTarget Event event, EventDtoRequest dto);

    default EventState mapUserEventAction(EventStateAction action) {
        return switch (action) {
            case CANCEL_REVIEW -> EventState.CANCELED;
            case SEND_TO_REVIEW -> EventState.PENDING;
            case null, default -> null;
        };
    }

    default EventState mapAdminEventAction(EventStateAction action) {
        return switch (action) {
            case PUBLISH_EVENT -> EventState.PUBLISHED;
            case REJECT_EVENT -> EventState.CANCELED;
            case null, default -> null;
        };
    }
}