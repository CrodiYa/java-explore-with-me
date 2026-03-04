package ru.practicum.ewm.controller.priv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.Formatter;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.GlobalExceptionHandler;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.category.CategoryDto;
import ru.practicum.ewm.model.comment.CommentDto;
import ru.practicum.ewm.model.comment.CommentDtoRequest;
import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.model.participation.ParticipationRequestDto;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.model.user.UserDto;
import ru.practicum.ewm.service.comment.CommentService;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.service.participation.ParticipationRequestService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PrivateEventController.class)
@ContextConfiguration(classes = {PrivateEventController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private ParticipationRequestService prService;

    private EventShortDto eventShortDto;
    private EventFullDto eventFullDto;
    private EventDtoRequest createRequest;
    private EventDtoRequest updateRequest;
    private ParticipationRequestDto requestDto;
    private EventRequestStatusUpdateRequest statusUpdateRequest;
    private EventRequestStatusUpdateResult updateResult;

    private final Long userId = 1L;
    private final Long eventId = 1L;
    private final String baseUrl = "/users/{userId}/events";

    private CommentDto commentDto;
    private CommentDtoRequest commentDtoRequest;
    private Long commentId = 1L;

    @BeforeEach
    public void setUp() {
        String date = Formatter.format(Instant.now().plus(3, ChronoUnit.HOURS));
        eventShortDto = EventShortDto.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test Annotation")
                .category(new CategoryDto(1L, "Test Category"))
                .initiator(new UserDto(userId, "Test User", "test@email.com"))
                .eventDate(date)
                .paid(false)
                .confirmedRequests(0)
                .views(0L)
                .build();

        eventFullDto = EventFullDto.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Test Annotation")
                .description("Test Description")
                .category(new CategoryDto(1L, "Test Category"))
                .initiator(new UserDto(userId, "Test User", "test@email.com"))
                .eventDate(date)
                .location(new Location(55.75, 37.62))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .confirmedRequests(0)
                .views(0L)
                .state(EventState.PENDING)
                .build();

        commentDto = CommentDto.builder()
                .id(commentId)
                .eventId(eventId).authorName("author")
                .text("Test comment")
                .created("2023-01-01 12:00:00")
                .updated("2023-01-01 12:00:00")
                .build();

        commentDtoRequest = new CommentDtoRequest();
        commentDtoRequest.setText("Test comment");

        createRequest = EventDtoRequest.builder()
                .title("New Event")
                .annotation("This is a valid annotation with enough length for testing")
                .description("This is a valid description with enough length for testing purposes")
                .eventDate(Instant.now().plus(3, ChronoUnit.HOURS).toString())
                .location(new Location(55.75, 37.62))
                .category(1L)
                .build();

        updateRequest = EventDtoRequest.builder()
                .title("Updated Event")
                .annotation("This is an updated annotation with enough length for testing")
                .description("This is an updated description with enough length for testing purposes")
                .eventDate(Instant.now().plus(5, ChronoUnit.HOURS).toString())
                .location(new Location(55.75, 37.62))
                .category(2L)
                .paid(true)
                .participantLimit(100)
                .requestModeration(false)
                .stateAction(EventStateAction.SEND_TO_REVIEW)
                .build();

        Long requestId = 1L;
        requestDto = ParticipationRequestDto.builder()
                .id(requestId)
                .requester(userId)
                .event(eventId)
                .created(Instant.now().toString())
                .status(ParticipationStatus.PENDING)
                .build();

        statusUpdateRequest = new EventRequestStatusUpdateRequest();
        statusUpdateRequest.setRequestIds(List.of(requestId));
        statusUpdateRequest.setStatus(ParticipationStatus.CONFIRMED);

        updateResult = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of(requestDto))
                .rejectedRequests(List.of())
                .build();
    }


    @Test
    public void shouldReturnEventsList() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);
        when(eventService.findEventsByUserId(eq(userId), anyInt(), anyInt()))
                .thenReturn(events);

        mockMvc.perform(get(baseUrl, userId)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Event")))
                .andExpect(jsonPath("$[0].annotation", is("Test Annotation")))
                .andExpect(jsonPath("$[0].paid", is(false)));
    }

    @Test
    public void shouldReturnEmptyListWhenNoEvents() throws Exception {
        when(eventService.findEventsByUserId(eq(userId), anyInt(), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get(baseUrl, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void shouldUseDefaultPaginationWhenNotProvided() throws Exception {
        when(eventService.findEventsByUserId(eq(userId), eq(0), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get(baseUrl, userId))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdNotPositiveGet() throws Exception {
        mockMvc.perform(get(baseUrl + "", 0))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenFromIsNegative() throws Exception {
        mockMvc.perform(get(baseUrl, userId)
                        .param("from", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenSizeIsZero() throws Exception {
        mockMvc.perform(get(baseUrl, userId)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnEvent() throws Exception {
        when(eventService.findEventById(userId, eventId))
                .thenReturn(eventFullDto);

        mockMvc.perform(get(baseUrl + "/{eventId}", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Event")))
                .andExpect(jsonPath("$.state", is("PENDING")))
                .andExpect(jsonPath("$.location.lat", is(55.75)))
                .andExpect(jsonPath("$.location.lon", is(37.62)));
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdNotPositiveGetId() throws Exception {
        mockMvc.perform(get(baseUrl + "/{eventId}", 0, eventId))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEventIdNotPositiveGetId() throws Exception {
        mockMvc.perform(get(baseUrl + "/{eventId}", userId, 0))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void shouldCreateEventSuccessfully() throws Exception {
        when(eventService.addEvent(userId, createRequest))
                .thenReturn(eventFullDto);

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Event")));
    }

    @Test
    public void shouldReturnBadRequestWhenTitleBlankPost() throws Exception {
        createRequest.setTitle("");

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenTitleTooShortPost() throws Exception {
        createRequest.setTitle("ab");

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenTitleTooLongPost() throws Exception {
        createRequest.setTitle("a".repeat(121));

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenAnnotationBlankPost() throws Exception {
        createRequest.setAnnotation("");

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenAnnotationTooShortPost() throws Exception {
        createRequest.setAnnotation("short");

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenAnnotationTooLong() throws Exception {
        createRequest.setAnnotation("a".repeat(2001));

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenDescriptionBlankPost() throws Exception {
        createRequest.setDescription("");

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenDescriptionTooShortPost() throws Exception {
        createRequest.setDescription("short");

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenDescriptionTooLong() throws Exception {
        createRequest.setDescription("a".repeat(7001));

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEventDateNull() throws Exception {
        createRequest.setEventDate(null);

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEventDateInvalidPost() throws Exception {
        createRequest.setEventDate("invalid-date");
        when(eventService.addEvent(userId, createRequest)).thenThrow(BadRequestException.class);

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenCategoryNull() throws Exception {
        createRequest.setCategory(null);

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenLocationNull() throws Exception {
        createRequest.setLocation(null);

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenLatitudeInvalid() throws Exception {
        createRequest.setLocation(new Location(-100.0, 37.62));

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenLongitudeInvalid() throws Exception {
        createRequest.setLocation(new Location(55.75, -200.0));

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdNotPositivePost() throws Exception {
        mockMvc.perform(post(baseUrl + "", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldThrowWhenStateActionOnCreate() throws Exception {
        createRequest.setStateAction(EventStateAction.PUBLISH_EVENT);

        when(eventService.addEvent(userId, createRequest))
                .thenReturn(eventFullDto);

        mockMvc.perform(post(baseUrl, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldPatchEventSuccessfully() throws Exception {
        EventFullDto updatedDto = EventFullDto.builder()
                .id(eventId)
                .title("Updated Event")
                .annotation("Updated Annotation")
                .description("Updated Description")
                .state(EventState.PENDING)
                .build();

        when(eventService.patchEvent(userId, eventId, updateRequest))
                .thenReturn(updatedDto);

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Updated Event")))
                .andExpect(jsonPath("$.state", is("PENDING")));
    }

    @Test
    public void shouldReturnBadRequestWhenTitleBlank() throws Exception {
        updateRequest.setTitle("");

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenTitleTooShort() throws Exception {
        updateRequest.setTitle("ab");

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenTitleTooLong() throws Exception {
        updateRequest.setTitle("a".repeat(121));

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenAnnotationBlank() throws Exception {
        updateRequest.setAnnotation("");

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenAnnotationTooShort() throws Exception {
        updateRequest.setAnnotation("short");

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenDescriptionBlank() throws Exception {
        updateRequest.setDescription("");

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenDescriptionTooShort() throws Exception {
        updateRequest.setDescription("short");

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEventDateInvalid() throws Exception {
        updateRequest.setEventDate("invalid-date");
        when(eventService.patchEvent(userId, eventId, updateRequest)).thenThrow(BadRequestException.class);

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenCategoryNotPositive() throws Exception {
        updateRequest.setCategory(-1L);

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenParticipantLimitNegative() throws Exception {
        updateRequest.setParticipantLimit(-1);

        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdNotPositivePatch() throws Exception {
        mockMvc.perform(patch(baseUrl + "/{eventId}", 0, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEventIdNotPositivePatch() throws Exception {
        mockMvc.perform(patch(baseUrl + "/{eventId}", userId, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void shouldReturnRequestsList() throws Exception {
        List<ParticipationRequestDto> requests = List.of(requestDto);
        when(prService.findByEventId(userId, eventId))
                .thenReturn(requests);

        mockMvc.perform(get(baseUrl + "/{eventId}/requests", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].requester", is(1)))
                .andExpect(jsonPath("$[0].event", is(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    public void shouldReturnEmptyListWhenNoRequests() throws Exception {
        when(prService.findByEventId(userId, eventId))
                .thenReturn(List.of());

        mockMvc.perform(get(baseUrl + "/{eventId}/requests", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdNotPositiveGetRequests() throws Exception {
        mockMvc.perform(get(baseUrl + "/{eventId}/requests", 0, eventId))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEventIdNotPositiveGetRequests() throws Exception {
        mockMvc.perform(get(baseUrl + "/{eventId}/requests", userId, 0))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void shouldConfirmRequestsSuccessfully() throws Exception {
        when(prService.updateStatusParticipationRequest(userId, eventId, statusUpdateRequest))
                .thenReturn(updateResult);

        mockMvc.perform(patch(baseUrl + "/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(1)))
                .andExpect(jsonPath("$.confirmedRequests[0].id", is(1)))
                .andExpect(jsonPath("$.confirmedRequests[0].status", is("PENDING")))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(0)));
    }

    @Test
    public void shouldRejectRequestsSuccessfully() throws Exception {
        statusUpdateRequest.setStatus(ParticipationStatus.REJECTED);

        EventRequestStatusUpdateResult rejectResult = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of())
                .rejectedRequests(List.of(requestDto))
                .build();

        when(prService.updateStatusParticipationRequest(userId, eventId, statusUpdateRequest))
                .thenReturn(rejectResult);

        mockMvc.perform(patch(baseUrl + "/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(0)))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(1)))
                .andExpect(jsonPath("$.rejectedRequests[0].id", is(1)));
    }

    @Test
    public void shouldReturnOkWhenRequestIdsEmpty() throws Exception {
        statusUpdateRequest.setRequestIds(List.of());

        mockMvc.perform(patch(baseUrl + "/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnBadRequestWhenStatusNull() throws Exception {
        statusUpdateRequest.setStatus(null);

        mockMvc.perform(patch(baseUrl + "/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenRequestBodyEmpty() throws Exception {
        mockMvc.perform(patch(baseUrl + "/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdNotPositive() throws Exception {
        mockMvc.perform(patch(baseUrl + "/{eventId}/requests", 0, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenEventIdNotPositive() throws Exception {
        mockMvc.perform(patch(baseUrl + "/{eventId}/requests", userId, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAddComment() throws Exception {
        when(commentService.addComment(eq(userId), eq(eventId), any(CommentDtoRequest.class)))
                .thenReturn(commentDto);

        mockMvc.perform(post(baseUrl + "/{eventId}/comment", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.authorName").value("author"))
                .andExpect(jsonPath("$.text").value("Test comment"));
    }

    @Test
    public void shouldReturnBadRequestWhenAddingCommentWithBlankText() throws Exception {
        commentDtoRequest.setText("");

        mockMvc.perform(post(baseUrl + "/{eventId}/comment", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenAddingCommentWithTextTooLong() throws Exception {
        String longText = "a".repeat(1001);
        commentDtoRequest.setText(longText);

        mockMvc.perform(post(baseUrl + "/{eventId}/comment", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenAddingCommentToUnpublishedEvent() throws Exception {
        when(commentService.addComment(eq(userId), eq(eventId), any(CommentDtoRequest.class)))
                .thenThrow(new BadRequestException("Событие еще не опубликовано"));

        mockMvc.perform(post(baseUrl + "/{eventId}/comment", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnNotFoundWhenAddingCommentToNonExistentEvent() throws Exception {
        when(commentService.addComment(eq(userId), eq(eventId), any(CommentDtoRequest.class)))
                .thenThrow(new NotFoundException("Событие с id " + eventId + " не найдено"));

        mockMvc.perform(post(baseUrl + "/{eventId}/comment", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnNotFoundWhenAddingCommentByNonExistentUser() throws Exception {
        when(commentService.addComment(eq(userId), eq(eventId), any(CommentDtoRequest.class)))
                .thenThrow(new NotFoundException("Пользователь с id " + userId + " не найден"));

        mockMvc.perform(post(baseUrl + "/{eventId}/comment", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdIsNegativeForAdd() throws Exception {
        mockMvc.perform(post(baseUrl + "/{eventId}/comment", -1, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldPatchComment() throws Exception {
        CommentDto updatedDto = new CommentDto();
        updatedDto.setId(commentId);
        updatedDto.setText("Updated text");

        CommentDtoRequest updateRequest = new CommentDtoRequest();
        updateRequest.setText("Updated text");

        when(commentService.patchComment(eq(userId), eq(eventId), eq(commentId), any(CommentDtoRequest.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(patch(baseUrl + "/{eventId}/comment/{commentId}", userId, eventId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.text").value("Updated text"));
    }

    @Test
    public void shouldReturnForbiddenWhenPatchingCommentByNonAuthor() throws Exception {
        when(commentService.patchComment(eq(userId), eq(eventId), eq(commentId), any(CommentDtoRequest.class)))
                .thenThrow(new ForbiddenException("Только автор может менять комментарии"));

        mockMvc.perform(patch(baseUrl + "/{eventId}/comment/{commentId}", userId, eventId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldReturnNotFoundWhenPatchingNonExistentComment() throws Exception {
        when(commentService.patchComment(eq(userId), eq(eventId), eq(999L), any(CommentDtoRequest.class)))
                .thenThrow(new NotFoundException("Комментарий с id 999 не найден"));

        mockMvc.perform(patch(baseUrl + "/{eventId}/comment/{commentId}", userId, eventId, 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWhenPatchingWithBlankText() throws Exception {
        commentDtoRequest.setText("");

        mockMvc.perform(patch(baseUrl + "/{eventId}/comment/{commentId}", userId, eventId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDtoRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldDeleteComment() throws Exception {
        doNothing().when(commentService).deleteComment(userId, eventId, commentId);

        mockMvc.perform(delete(baseUrl + "/{eventId}/comment/{commentId}", userId, eventId, commentId))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturnForbiddenWhenDeletingCommentByNonAuthor() throws Exception {
        doThrow(new ForbiddenException("Только автор может удалять комментарии"))
                .when(commentService).deleteComment(userId, eventId, commentId);

        mockMvc.perform(delete(baseUrl + "/{eventId}/comment/{commentId}", userId, eventId, commentId))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingNonExistentComment() throws Exception {
        doThrow(new NotFoundException("Комментарий с id " + commentId + " не найден"))
                .when(commentService).deleteComment(userId, eventId, commentId);

        mockMvc.perform(delete(baseUrl + "/{eventId}/comment/{commentId}", userId, eventId, commentId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdIsNegativeForDelete() throws Exception {
        mockMvc.perform(delete(baseUrl + "/{eventId}/comment/{commentId}", -1, eventId, commentId))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldGetUserCommentsWithDefaultParams() throws Exception {
        List<CommentDto> comments = List.of(commentDto);
        when(commentService.getUserComments(eq(userId), eq("DESC"), eq(0), eq(10)))
                .thenReturn(comments);

        mockMvc.perform(get(baseUrl + "/comment", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$[0].id").value(commentId))
                .andExpect(jsonPath("$[0].eventId").value(eventId))
                .andExpect(jsonPath("$[0].authorName").value("author"))
                .andExpect(jsonPath("$[0].text").value("Test comment"));
    }

    @Test
    public void shouldGetUserCommentsWithCustomParams() throws Exception {
        List<CommentDto> comments = List.of(commentDto);
        when(commentService.getUserComments(eq(userId), eq("ASC"), eq(5), eq(20)))
                .thenReturn(comments);

        mockMvc.perform(get(baseUrl + "/comment", userId)
                        .param("sort", "ASC")
                        .param("from", "5")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$[0].id").value(commentId));
    }

    @Test
    public void shouldReturnEmptyListWhenNoUserComments() throws Exception {
        when(commentService.getUserComments(eq(userId), eq("DESC"), eq(0), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get(baseUrl + "/comment", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void shouldReturnNotFoundWhenUserDoesNotExistForGetComments() throws Exception {
        when(commentService.getUserComments(eq(userId), eq("DESC"), eq(0), eq(10)))
                .thenThrow(new NotFoundException("Пользователь с id " + userId + " не найден"));

        mockMvc.perform(get(baseUrl + "/comment", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWhenUserIdIsNegativeForGetComments() throws Exception {
        mockMvc.perform(get(baseUrl + "/comment", -1))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenFromIsNegativeForGetComments() throws Exception {
        mockMvc.perform(get(baseUrl + "/comment", userId)
                        .param("from", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenSizeIsZeroForGetComments() throws Exception {
        mockMvc.perform(get(baseUrl + "/comment", userId)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenSizeIsNegativeForGetComments() throws Exception {
        mockMvc.perform(get(baseUrl + "/comment", userId)
                        .param("size", "-10"))
                .andExpect(status().isBadRequest());
    }
}
