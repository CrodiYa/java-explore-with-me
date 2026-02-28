package ru.practicum.ewm.service.participation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.ParticipationRequestMapper;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.model.event.EventRequestStatusUpdateResult;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.participation.ParticipationRequest;
import ru.practicum.ewm.model.participation.ParticipationRequestDto;
import ru.practicum.ewm.model.participation.ParticipationState;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.service.user.UserService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository repository;
    private final ParticipationRequestMapper mapper;
    private final UserService userService;
    private final EventService eventService;

    @Override
    public List<ParticipationRequestDto> findByRequesterId(Long requesterId) {
        return repository.findByRequesterId(requesterId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        User requester = userService.findEntityById(userId);
        Event event = eventService.findEntityById(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя учавствовать в неопубликованном событии");
        }

        if (repository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Такой владелец уже есть");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        // если количество подтв ивентов больше или равно лимиту и сам лимит не равен 0 то
        if (repository.countByEventIdAndStatus(eventId, ParticipationState.CONFIRMED) >= event.getParticipantLimit()
                && event.getParticipantLimit() != 0) {
            throw new ConflictException("Достигнут лимит запросов на участие");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requester(requester)
                .event(event)
                .status(ParticipationState.PENDING)
                .build();

        if (!event.getRequestModeration())
            request.setStatus(ParticipationState.CONFIRMED);

        return mapper.toDto(repository.save(request));
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        ParticipationRequest request = repository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Заявка не найдена"));
        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Нельзя отменить чужую заявку");
        }
        request.setStatus(ParticipationState.CANCELED);
        return mapper.toDto(repository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> findByEventId(Long userId, Long eventId) {
        getEventByOwner(userId, eventId);
        return repository.findByEventId(eventId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public EventRequestStatusUpdateResult updateStatusParticipationRequest(Long userId, Long eventId,
                                                                           EventRequestStatusUpdateRequest request) {
        Event event = getEventByOwner(userId, eventId);
        // проверяем лимиты после раннего выхода
        int limit = event.getParticipantLimit();
        int countConfirmed = repository.countByEventIdAndStatus(eventId, ParticipationState.CONFIRMED);
        List<ParticipationRequest> requests = repository.findAllByIdIn(request.getRequestIds());
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        log.info("requestModeration={}, participantLimit={}", event.getRequestModeration(),
                event.getParticipantLimit());

        // если модерация выключена (все идет автоматически) или лимита на запросы нет
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(List.of())
                    .build();
        }

        // владелец хочет подтвердить, а лимит исчерпан
        // при отклонении не важно, отклонять можно всегда
        if (request.getStatus() == ParticipationState.CONFIRMED && countConfirmed >= limit)
            throw new ConflictException("Достигнут лимит подтвержденных заявок");

        for (ParticipationRequest pr : requests) {
            if (!pr.getStatus().equals(ParticipationState.PENDING))
                throw new ConflictException("Статус можно изменить только у заявок в состоянии рассмотрения");

            // request.getStatus() -  тело запроса от владельца.
            // Он говорит "хочу эти заявки подтвердить" или "хочу отклонить"
            if (request.getStatus() == ParticipationState.CONFIRMED && countConfirmed < limit) {
                pr.setStatus(ParticipationState.CONFIRMED);
                countConfirmed++;
                confirmedRequests.add(mapper.toDto(pr));
            } else {
                pr.setStatus(ParticipationState.REJECTED);
                rejectedRequests.add(mapper.toDto(pr));
            }
        }

        repository.saveAll(requests);

        // если лимит исчерпался во время цикла, дошел до края цикла (пограничный случай)
        // Если при подтверждении данной заявки, лимит заявок для события исчерпан
        // то все неподтверждённые заявки необходимо отклонить
        if (request.getStatus().equals(ParticipationState.CONFIRMED) && countConfirmed >= limit) {
            // ожидающие подтверждения
            List<ParticipationRequest> pendingRequests =
                    repository.findByEventIdAndStatus(eventId, ParticipationState.PENDING);
            pendingRequests.forEach(pr -> pr.setStatus(ParticipationState.REJECTED));
            repository.saveAll(pendingRequests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    private Event getEventByOwner(Long userId, Long eventId) {
        Event event = eventService.findEntityById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не найдено");
        }
        return event;
    }
}
