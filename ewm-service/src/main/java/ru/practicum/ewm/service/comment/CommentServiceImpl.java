package ru.practicum.ewm.service.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.CommentMapper;
import ru.practicum.ewm.model.comment.Comment;
import ru.practicum.ewm.model.comment.CommentDto;
import ru.practicum.ewm.model.comment.CommentDtoRequest;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.service.user.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final EventService eventService;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;


    @Override
    public CommentDto addComment(Long userId, Long eventId, CommentDtoRequest request) {
        Event event = eventService.findEntityById(eventId);

        throwIfEventNotPublished(event);

        User user = userService.findEntityById(userId);

        Comment comment = commentMapper.toComment(request);
        comment.setAuthor(user);
        comment.setEventId(eventId);

        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    public CommentDto patchComment(Long userId, Long eventId, Long commentId, CommentDtoRequest request) {
        userService.throwIfUserNotFound(userId);
        eventService.throwIfEventNotFound(eventId);

        Comment comment = findEntityById(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Только автор может менять комментарии");
        }

        comment.setText(request.getText());

        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    public void deleteComment(Long userId, Long eventId, Long commentId) {
        userService.throwIfUserNotFound(userId);
        eventService.throwIfEventNotFound(eventId);

        Comment comment = findEntityById(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Только автор может удалять комментарии");
        }

        commentRepository.deleteById(commentId);
    }

    @Override
    public void deleteCommentAdmin(Long eventId, Long commentId) {
        eventService.throwIfEventNotFound(eventId);
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий с id " + commentId + " не найден");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, String sort, Integer from, Integer size) {
        userService.throwIfUserNotFound(userId);

        Sort sorting = getSorting(sort);

        return commentRepository.findByAuthorId(userId, PageRequest.of(from / size, size, sorting)).stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, String sort, Integer from, Integer size) {
        Event event = eventService.findEntityById(eventId);
        throwIfEventNotPublished(event);

        Sort sorting = getSorting(sort);

        return commentRepository.findByEventId(eventId, PageRequest.of(from / size, size, sorting)).stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @Override
    public CommentDto getCommentById(Long eventId, Long commentId) {
        Event event = eventService.findEntityById(eventId);
        throwIfEventNotPublished(event);

        return commentMapper.toDto(findEntityById(commentId));
    }

    private Comment findEntityById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id " + commentId + " не найден"));
    }

    private Sort getSorting(String sort) {
        Sort sorting = Sort.by("created").descending();
        if (sort.equals("ASC")) {
            sorting = sorting.ascending();
        }

        return sorting;
    }

    private void throwIfEventNotPublished(Event event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new BadRequestException("Событие еще не опубликовано");
        }
    }
}
