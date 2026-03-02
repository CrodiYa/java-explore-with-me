package ru.practicum.ewm.service.participation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.ParticipationRequestMapper;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.model.event.EventRequestStatusUpdateResult;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.participation.ParticipationRequest;
import ru.practicum.ewm.model.participation.ParticipationRequestDto;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.service.user.UserService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParticipationRequestServiceImplTest {

    @Mock
    private ParticipationRequestRepository repository;

    @Mock
    private ParticipationRequestMapper mapper;

    @Mock
    private UserService userService;

    @Mock
    private EventService eventService;

    @InjectMocks
    private ParticipationRequestServiceImpl service;

    @Captor
    private ArgumentCaptor<ParticipationRequest> requestCaptor;

    private User user;
    private User initiator;
    private Event event;
    private ParticipationRequest request;
    private ParticipationRequestDto requestDto;
    private final Long userId = 1L;
    private final Long initiatorId = 2L;
    private final Long eventId = 1L;
    private final Long requestId = 1L;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(userId);
        user.setName("Test User");

        initiator = new User();
        initiator.setId(initiatorId);
        initiator.setName("Initiator");

        event = new Event();
        event.setId(eventId);
        event.setInitiator(initiator);
        event.setState(EventState.PUBLISHED);
        event.setRequestModeration(true);
        event.setParticipantLimit(10);

        request = ParticipationRequest.builder()
                .id(requestId)
                .requester(user)
                .event(event)
                .status(ParticipationStatus.PENDING)
                .created(Instant.now())
                .build();

        requestDto = ParticipationRequestDto.builder()
                .id(requestId)
                .requester(userId)
                .event(eventId)
                .status(ParticipationStatus.PENDING)
                .build();
    }

    @Nested
    class FindingByRequesterId {

        @Test
        public void shouldReturnRequestsList() {
            when(repository.findByRequesterId(userId)).thenReturn(List.of(request));
            when(mapper.toDto(request)).thenReturn(requestDto);

            List<ParticipationRequestDto> result = service.findByRequesterId(userId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(requestId);
        }

        @Test
        public void shouldReturnEmptyListWhenNoRequests() {
            when(repository.findByRequesterId(userId)).thenReturn(List.of());

            List<ParticipationRequestDto> result = service.findByRequesterId(userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class AddingParticipationRequest {

        @Test
        public void shouldAddRequestSuccessfully() {
            when(userService.findEntityById(userId)).thenReturn(user);
            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.existsByRequesterIdAndEventId(userId, eventId)).thenReturn(false);
            when(repository.save(any(ParticipationRequest.class))).thenReturn(request);
            when(mapper.toDto(request)).thenReturn(requestDto);

            ParticipationRequestDto result = service.addParticipationRequest(userId, eventId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(requestId);
            verify(repository).save(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getStatus()).isEqualTo(ParticipationStatus.PENDING);
        }

        @Test
        public void shouldAutoConfirmWhenModerationOff() {
            event.setRequestModeration(false);

            when(userService.findEntityById(userId)).thenReturn(user);
            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.existsByRequesterIdAndEventId(userId, eventId)).thenReturn(false);
            when(repository.save(any(ParticipationRequest.class))).thenReturn(request);
            when(mapper.toDto(request)).thenReturn(requestDto);

            service.addParticipationRequest(userId, eventId);

            verify(repository).save(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        }

        @Test
        public void shouldAutoConfirmWhenLimitZero() {
            event.setParticipantLimit(0);

            when(userService.findEntityById(userId)).thenReturn(user);
            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.existsByRequesterIdAndEventId(userId, eventId)).thenReturn(false);
            when(repository.save(any(ParticipationRequest.class))).thenReturn(request);
            when(mapper.toDto(request)).thenReturn(requestDto);

            service.addParticipationRequest(userId, eventId);

            verify(repository).save(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        }

        @Test
        public void shouldThrowConflictWhenEventNotPublished() {
            event.setState(EventState.PENDING);

            when(userService.findEntityById(userId)).thenReturn(user);
            when(eventService.findEntityById(eventId)).thenReturn(event);

            assertThatThrownBy(() -> service.addParticipationRequest(userId, eventId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Нельзя участвовать в неопубликованном событии");
        }

        @Test
        public void shouldThrowConflictWhenRequestAlreadyExists() {
            when(userService.findEntityById(userId)).thenReturn(user);
            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.existsByRequesterIdAndEventId(userId, eventId)).thenReturn(true);

            assertThatThrownBy(() -> service.addParticipationRequest(userId, eventId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Запрос уже существует");
        }

        @Test
        public void shouldThrowConflictWhenUserIsInitiator() {
            event.setInitiator(user);

            when(userService.findEntityById(userId)).thenReturn(user);
            when(eventService.findEntityById(eventId)).thenReturn(event);

            assertThatThrownBy(() -> service.addParticipationRequest(userId, eventId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Инициатор события не может добавить запрос на участие в своём событии");
        }

        @Test
        public void shouldThrowConflictWhenLimitReached() {
            when(userService.findEntityById(userId)).thenReturn(user);
            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.existsByRequesterIdAndEventId(userId, eventId)).thenReturn(false);
            when(repository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED))
                    .thenReturn(10);

            assertThatThrownBy(() -> service.addParticipationRequest(userId, eventId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Достигнут лимит запросов на участие");
        }
    }

    @Nested
    class CancelingParticipationRequest {

        @Test
        public void shouldCancelRequestSuccessfully() {
            when(repository.findById(requestId)).thenReturn(Optional.of(request));
            when(repository.save(any(ParticipationRequest.class))).thenReturn(request);
            when(mapper.toDto(request)).thenReturn(requestDto);

            ParticipationRequestDto result = service.cancelParticipationRequest(userId, requestId);

            assertThat(result).isNotNull();
            verify(repository).save(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getStatus()).isEqualTo(ParticipationStatus.CANCELED);
        }

        @Test
        public void shouldThrowNotFoundWhenRequestNotExists() {
            when(repository.findById(requestId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelParticipationRequest(userId, requestId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Заявка не найдена");
        }

        @Test
        public void shouldThrowConflictWhenCancelingOthersRequest() {
            User otherUser = new User();
            otherUser.setId(3L);
            request.setRequester(otherUser);

            when(repository.findById(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.cancelParticipationRequest(userId, requestId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Нельзя отменить чужую заявку");
        }
    }

    @Nested
    class FindingByEventId {

        @Test
        public void shouldReturnRequestsForEvent() {
            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.findByEventId(eventId)).thenReturn(List.of(request));
            when(mapper.toDto(request)).thenReturn(requestDto);

            List<ParticipationRequestDto> result = service.findByEventId(initiatorId, eventId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(requestId);
        }

        @Test
        public void shouldThrowNotFoundWhenUserNotInitiator() {
            when(eventService.findEntityById(eventId)).thenReturn(event);

            assertThatThrownBy(() -> service.findByEventId(3L, eventId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Событие не найдено");
        }
    }

    @Nested
    class UpdatingStatusParticipationRequest {

        @Test
        public void shouldConfirmRequestsSuccessfully() {
            EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
            updateRequest.setRequestIds(List.of(requestId));
            updateRequest.setStatus(ParticipationStatus.CONFIRMED);

            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED)).thenReturn(5);
            when(repository.findAllByIdIn(List.of(requestId))).thenReturn(List.of(request));
            when(repository.saveAll(anyList())).thenReturn(List.of(request));
            when(mapper.toDto(request)).thenReturn(requestDto);

            EventRequestStatusUpdateResult result = service.updateStatusParticipationRequest(
                    initiatorId, eventId, updateRequest);

            assertThat(result.getConfirmedRequests()).hasSize(1);
            assertThat(result.getRejectedRequests()).isEmpty();
            assertThat(request.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        }

        @Test
        public void shouldRejectRequestsSuccessfully() {
            EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
            updateRequest.setRequestIds(List.of(requestId));
            updateRequest.setStatus(ParticipationStatus.REJECTED);

            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED)).thenReturn(5);
            when(repository.findAllByIdIn(List.of(requestId))).thenReturn(List.of(request));
            when(repository.saveAll(anyList())).thenReturn(List.of(request));
            when(mapper.toDto(request)).thenReturn(requestDto);

            EventRequestStatusUpdateResult result = service.updateStatusParticipationRequest(
                    initiatorId, eventId, updateRequest);

            assertThat(result.getConfirmedRequests()).isEmpty();
            assertThat(result.getRejectedRequests()).hasSize(1);
            assertThat(request.getStatus()).isEqualTo(ParticipationStatus.REJECTED);
        }

        @Test
        public void shouldReturnEmptyWhenModerationOff() {
            event.setRequestModeration(false);
            EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
            updateRequest.setRequestIds(List.of(requestId));
            updateRequest.setStatus(ParticipationStatus.CONFIRMED);

            when(eventService.findEntityById(eventId)).thenReturn(event);

            EventRequestStatusUpdateResult result = service.updateStatusParticipationRequest(
                    initiatorId, eventId, updateRequest);

            assertThat(result.getConfirmedRequests()).isEmpty();
            assertThat(result.getRejectedRequests()).isEmpty();
            verify(repository, never()).findAllByIdIn(anyList());
        }

        @Test
        public void shouldReturnEmptyWhenLimitZero() {
            event.setParticipantLimit(0);
            EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
            updateRequest.setRequestIds(List.of(requestId));
            updateRequest.setStatus(ParticipationStatus.CONFIRMED);

            when(eventService.findEntityById(eventId)).thenReturn(event);

            EventRequestStatusUpdateResult result = service.updateStatusParticipationRequest(
                    initiatorId, eventId, updateRequest);

            assertThat(result.getConfirmedRequests()).isEmpty();
            assertThat(result.getRejectedRequests()).isEmpty();
            verify(repository, never()).findAllByIdIn(anyList());
        }

        @Test
        public void shouldThrowConflictWhenLimitReachedForConfirm() {
            EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
            updateRequest.setRequestIds(List.of(requestId));
            updateRequest.setStatus(ParticipationStatus.CONFIRMED);

            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED)).thenReturn(10);

            assertThatThrownBy(() -> service.updateStatusParticipationRequest(
                    initiatorId, eventId, updateRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Достигнут лимит подтвержденных заявок");
        }

        @Test
        public void shouldThrowConflictWhenRequestNotPending() {
            request.setStatus(ParticipationStatus.CONFIRMED);
            EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
            updateRequest.setRequestIds(List.of(requestId));
            updateRequest.setStatus(ParticipationStatus.CONFIRMED);

            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED)).thenReturn(5);
            when(repository.findAllByIdIn(List.of(requestId))).thenReturn(List.of(request));

            assertThatThrownBy(() -> service.updateStatusParticipationRequest(
                    initiatorId, eventId, updateRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Статус можно изменить только у заявок в состоянии рассмотрения");
        }

        @Test
        public void shouldRejectRemainingWhenLimitReachedDuringConfirmation() {
            ParticipationRequest request2 = ParticipationRequest.builder()
                    .id(2L)
                    .requester(user)
                    .event(event)
                    .status(ParticipationStatus.PENDING)
                    .build();

            EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
            updateRequest.setRequestIds(List.of(requestId, 2L));
            updateRequest.setStatus(ParticipationStatus.CONFIRMED);

            when(eventService.findEntityById(eventId)).thenReturn(event);
            when(repository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED)).thenReturn(9);
            when(repository.findAllByIdIn(List.of(requestId, 2L))).thenReturn(List.of(request, request2));
            when(repository.saveAll(anyList())).thenReturn(List.of(request, request2));
            when(mapper.toDto(request)).thenReturn(requestDto);

            ParticipationRequestDto requestDto2 = ParticipationRequestDto.builder()
                    .id(2L)
                    .requester(userId)
                    .event(eventId)
                    .status(ParticipationStatus.REJECTED)
                    .build();
            when(mapper.toDto(request2)).thenReturn(requestDto2);

            EventRequestStatusUpdateResult result = service.updateStatusParticipationRequest(
                    initiatorId, eventId, updateRequest);

            assertThat(result.getConfirmedRequests()).hasSize(1);
            assertThat(result.getRejectedRequests()).hasSize(1);
            assertThat(request.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
            assertThat(request2.getStatus()).isEqualTo(ParticipationStatus.REJECTED);
            verify(repository).rejectPendingRequests(eventId, ParticipationStatus.PENDING);
        }
    }
}