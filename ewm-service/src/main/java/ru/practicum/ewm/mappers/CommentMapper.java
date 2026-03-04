package ru.practicum.ewm.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.Formatter;
import ru.practicum.ewm.model.comment.Comment;
import ru.practicum.ewm.model.comment.CommentDto;
import ru.practicum.ewm.model.comment.CommentDtoRequest;

@Mapper(componentModel = "spring", imports = Formatter.class)
public interface CommentMapper {

    @Mapping(source = "author.name", target = "authorName")
    @Mapping(target = "created", expression = "java(Formatter.format(comment.getCreated()))")
    @Mapping(target = "updated", expression = "java(comment.getUpdated() == null ? null : Formatter.format(comment.getUpdated()))")
    CommentDto toDto(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    Comment toComment(CommentDtoRequest dto);
}
