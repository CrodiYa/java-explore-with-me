package ru.practicum.ewm.mappers;

import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.model.response.CategoryDto;
import ru.practicum.ewm.model.response.UserDto;

import java.time.Instant;

import static ru.practicum.dto.Formatter.format;
import static ru.practicum.dto.Formatter.toInstant;

public class EventMapper {
    public static EventFullDto toFullDto(Event event) {
        Location location = new Location(event.getLat(), event.getLon());

        UserDto initiator = UserMapper.toDto(event.getInitiator());
        CategoryDto category = CategoryMapper.toDto(event.getCategory());

        String eventDate = format(event.getEventDate());
        String createdOn = format(event.getCreatedOn());
        String publishedOn = event.getPublishedOn() == null ? null : format(event.getPublishedOn());

        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .location(location)
                .category(category)
                .initiator(initiator)
                .eventDate(eventDate)
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .createdOn(createdOn)
                .publishedOn(publishedOn)
                .build();
    }

    public static EventShortDto toShortDto(Event event) {
        UserDto initiator = UserMapper.toDto(event.getInitiator());
        CategoryDto category = CategoryMapper.toDto(event.getCategory());
        String eventDate = format(event.getEventDate());

        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .paid(event.getPaid())
                .category(category)
                .initiator(initiator)
                .eventDate(eventDate)
                .build();
    }

    public static Event toEvent(EventDtoRequest dto) {
        Instant eventDate = toInstant(dto.getEventDate());

        return Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(eventDate)
                .lat(dto.getLocation().lat())
                .lon(dto.getLocation().lon())
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration())
                .build();
    }

    /**
     * Merges non-null fields from the DTO into the existing event entity.
     * <p>Only updates fields where the DTO value is not null, preserving existing values
     * for fields that are not provided in the update request.
     *
     * <p>This method updates the following fields if present in the DTO:
     * <ul>
     *   <li>title - event title</li>
     *   <li>annotation</li>
     *   <li>description</li>
     *   <li>eventDate</li>
     *   <li>lat</li>
     *   <li>lon</li>
     *   <li>paid</li>
     *   <li>participantLimit</li>
     *   <li>requestModeration</li>
     * </ul>
     *
     * @param event the target event entity to update, not null
     * @param dto   the source DTO containing the new field values, not null
     */
    public static void merge(Event event, EventDtoRequest dto) {
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }

        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }

        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }

        if (dto.getEventDate() != null) {
            event.setEventDate(toInstant(dto.getEventDate()));
        }

        if (dto.getLocation() != null) {
            event.setLat(dto.getLocation().lat());
            event.setLon(dto.getLocation().lon());
        }

        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
    }

    public static EventState mapUserEventAction(EventStateAction action) {
        return switch (action) {
            case CANCEL_REVIEW -> EventState.CANCELED;
            case SEND_TO_REVIEW -> EventState.PENDING;
            case null, default -> null;
        };
    }

    public static EventState mapAdminEventAction(EventStateAction action) {
        return switch (action) {
            case PUBLISH_EVENT -> EventState.PUBLISHED;
            case REJECT_EVENT -> EventState.REJECT;
            case null, default -> null;
        };
    }
}