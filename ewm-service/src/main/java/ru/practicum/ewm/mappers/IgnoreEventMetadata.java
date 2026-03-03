package ru.practicum.ewm.mappers;

import org.mapstruct.Mapping;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
@Mapping(target = "id", ignore = true)
@Mapping(target = "initiator", ignore = true)
@Mapping(target = "category", ignore = true)
@Mapping(target = "state", ignore = true)
@Mapping(target = "publishedOn", ignore = true)
@Mapping(target = "createdOn", ignore = true)
public @interface IgnoreEventMetadata {
}
