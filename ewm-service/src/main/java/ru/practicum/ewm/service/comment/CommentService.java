package ru.practicum.ewm.service.comment;

import ru.practicum.ewm.model.comment.CommentDto;
import ru.practicum.ewm.model.comment.CommentDtoRequest;

import java.util.List;

public interface CommentService {

    CommentDto addComment(Long userId, Long eventId, CommentDtoRequest request);

    CommentDto patchComment(Long userId, Long eventId, Long commentId, CommentDtoRequest request);

    void deleteComment(Long userId, Long eventId, Long commentId);

    void deleteCommentAdmin(Long eventId, Long commentId);

    List<CommentDto> getUserComments(Long userId, String sort, Integer from, Integer size);

    List<CommentDto> getEventComments(Long eventId, String sort, Integer from, Integer size);

    CommentDto getCommentById(Long eventId, Long commentId);
}
