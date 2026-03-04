package ru.practicum.ewm.service.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceImplTest {

    @Mock
    private EventService eventService;

    @Mock
    private UserService userService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User user;
    private Event event;
    private Comment comment;
    private CommentDto commentDto;
    private CommentDtoRequest commentDtoRequest;
    private Long userId = 1L;
    private Long eventId = 1L;
    private Long commentId = 1L;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(userId);

        event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);

        comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(user);
        comment.setEventId(eventId);
        comment.setText("Test comment");

        commentDto = new CommentDto();
        commentDto.setId(commentId);
        commentDto.setAuthorName("author");
        commentDto.setText("Test comment");

        commentDtoRequest = new CommentDtoRequest();
        commentDtoRequest.setText("Test comment");
    }

    @Test
    public void shouldAddComment() {
        when(eventService.findEntityById(eventId)).thenReturn(event);
        when(userService.findEntityById(userId)).thenReturn(user);
        when(commentMapper.toComment(commentDtoRequest)).thenReturn(comment);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.addComment(userId, eventId, commentDtoRequest);

        assertNotNull(result);
        assertEquals(commentDto, result);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    public void shouldThrowBadRequestWhenAddingCommentToUnpublishedEvent() {
        event.setState(EventState.PENDING);
        when(eventService.findEntityById(eventId)).thenReturn(event);

        assertThrows(BadRequestException.class,
                () -> commentService.addComment(userId, eventId, commentDtoRequest));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    public void shouldPatchComment() {
        doNothing().when(userService).throwIfUserNotFound(userId);
        doNothing().when(eventService).throwIfEventNotFound(eventId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDtoRequest updateRequest = new CommentDtoRequest();
        updateRequest.setText("Updated text");

        CommentDto result = commentService.patchComment(userId, eventId, commentId, updateRequest);

        assertNotNull(result);
        assertEquals("Updated text", comment.getText());
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    public void shouldThrowForbiddenWhenPatchingCommentByNonAuthor() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        comment.setAuthor(anotherUser);

        doNothing().when(userService).throwIfUserNotFound(userId);
        doNothing().when(eventService).throwIfEventNotFound(eventId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenException.class,
                () -> commentService.patchComment(userId, eventId, commentId, commentDtoRequest));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    public void shouldThrowNotFoundWhenPatchingNonExistentComment() {
        doNothing().when(userService).throwIfUserNotFound(userId);
        doNothing().when(eventService).throwIfEventNotFound(eventId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.patchComment(userId, eventId, commentId, commentDtoRequest));
    }

    @Test
    public void shouldDeleteComment() {
        doNothing().when(userService).throwIfUserNotFound(userId);
        doNothing().when(eventService).throwIfEventNotFound(eventId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        doNothing().when(commentRepository).deleteById(commentId);

        commentService.deleteComment(userId, eventId, commentId);

        verify(commentRepository, times(1)).deleteById(commentId);
    }

    @Test
    public void shouldThrowForbiddenWhenDeletingCommentByNonAuthor() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        comment.setAuthor(anotherUser);
        doNothing().when(userService).throwIfUserNotFound(userId);
        doNothing().when(eventService).throwIfEventNotFound(eventId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenException.class,
                () -> commentService.deleteComment(userId, eventId, commentId));
        verify(commentRepository, never()).deleteById(anyLong());
    }

    @Test
    public void shouldDeleteCommentAdmin() {
        doNothing().when(eventService).throwIfEventNotFound(eventId);

        when(commentRepository.existsById(commentId)).thenReturn(true);
        doNothing().when(commentRepository).deleteById(commentId);

        commentService.deleteCommentAdmin(eventId, commentId);

        verify(commentRepository, times(1)).deleteById(commentId);
    }

    @Test
    public void shouldThrowNotFoundWhenDeletingNonExistentCommentAdmin() {
        doNothing().when(eventService).throwIfEventNotFound(eventId);

        when(commentRepository.existsById(commentId)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> commentService.deleteCommentAdmin(eventId, commentId));
        verify(commentRepository, never()).deleteById(anyLong());
    }

    @Test
    public void shouldGetUserCommentsWithDefaultSort() {
        doNothing().when(userService).throwIfUserNotFound(userId);

        when(commentRepository.findByAuthorId(eq(userId), any(PageRequest.class)))
                .thenReturn(List.of(comment));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        List<CommentDto> result = commentService.getUserComments(userId, "DESC", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commentDto, result.getFirst());
    }

    @Test
    public void shouldGetUserCommentsWithAscendingSort() {
        doNothing().when(userService).throwIfUserNotFound(userId);
        when(commentRepository.findByAuthorId(eq(userId), any(PageRequest.class)))
                .thenReturn(List.of(comment));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        List<CommentDto> result = commentService.getUserComments(userId, "ASC", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commentDto, result.getFirst());
    }

    @Test
    public void shouldGetEventComments() {
        when(eventService.findEntityById(eventId)).thenReturn(event);
        when(commentRepository.findByEventId(eq(eventId), any(PageRequest.class)))
                .thenReturn(List.of(comment));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        List<CommentDto> result = commentService.getEventComments(eventId, "DESC", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commentDto, result.getFirst());
    }

    @Test
    public void shouldThrowBadRequestWhenGettingCommentsForUnpublishedEvent() {
        event.setState(EventState.PENDING);
        when(eventService.findEntityById(eventId)).thenReturn(event);

        assertThrows(BadRequestException.class,
                () -> commentService.getEventComments(eventId, "DESC", 0, 10));
    }

    @Test
    public void shouldGetCommentById() {
        when(eventService.findEntityById(eventId)).thenReturn(event);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.getCommentById(eventId, commentId);

        assertNotNull(result);
        assertEquals(commentDto, result);
    }

    @Test
    public void shouldThrowBadRequestWhenGettingCommentForUnpublishedEvent() {
        event.setState(EventState.PENDING);
        when(eventService.findEntityById(eventId)).thenReturn(event);

        assertThrows(BadRequestException.class,
                () -> commentService.getCommentById(eventId, commentId));
    }

    @Test
    public void shouldThrowNotFoundWhenGettingNonExistentComment() {
        when(eventService.findEntityById(eventId)).thenReturn(event);
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.getCommentById(eventId, commentId));
    }
}