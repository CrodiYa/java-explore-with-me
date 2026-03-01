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
import ru.practicum.ewm.model.participation.ParticipationStatus;
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

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Нельзя учавствовать в неопубликованном событии");
        }

        if (repository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Запрос уже существует");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        // если количество подтв ивентов больше или равно лимиту и сам лимит не равен 0 то
        if (event.getParticipantLimit() != 0
                && repository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED)
                >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит запросов на участие");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requester(requester)
                .event(event)
                .status(ParticipationStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationStatus.CONFIRMED);
        }

        return mapper.toDto(repository.save(request));
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        ParticipationRequest request = repository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Заявка не найдена"));
        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Нельзя отменить чужую заявку");
        }
        request.setStatus(ParticipationStatus.CANCELED);
        return mapper.toDto(repository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> findByEventId(Long userId, Long eventId) {
        getEventAndVerifyOwner(userId, eventId);
        return repository.findByEventId(eventId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public EventRequestStatusUpdateResult updateStatusParticipationRequest(Long userId, Long eventId,
                                                                           EventRequestStatusUpdateRequest request) {
        Event event = getEventAndVerifyOwner(userId, eventId);
        // проверяем лимиты после раннего выхода
        int limit = event.getParticipantLimit();

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

        int countConfirmed = repository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);
        List<ParticipationRequest> requests = repository.findAllByIdIn(request.getRequestIds());

        // владелец хочет подтвердить, а лимит исчерпан
        // при отклонении не важно, отклонять можно всегда
        if (request.getStatus().equals(ParticipationStatus.CONFIRMED) && countConfirmed >= limit)
            throw new ConflictException("Достигнут лимит подтвержденных заявок");

        for (ParticipationRequest pr : requests) {
            if (!pr.getStatus().equals(ParticipationStatus.PENDING))
                throw new ConflictException("Статус можно изменить только у заявок в состоянии рассмотрения");

            // request.getStatus() -  тело запроса от владельца.
            // Он говорит "хочу эти заявки подтвердить" или "хочу отклонить"
            if (request.getStatus().equals(ParticipationStatus.CONFIRMED) && countConfirmed < limit) {
                pr.setStatus(ParticipationStatus.CONFIRMED);
                countConfirmed++;
                confirmedRequests.add(mapper.toDto(pr));
            } else {
                pr.setStatus(ParticipationStatus.REJECTED);
                rejectedRequests.add(mapper.toDto(pr));
            }
        }

        repository.saveAll(requests);

        // если лимит исчерпался во время цикла, дошел до края цикла (пограничный случай)
        // Если при подтверждении данной заявки, лимит заявок для события исчерпан
        // то все неподтверждённые заявки необходимо отклонить
        if (request.getStatus().equals(ParticipationStatus.CONFIRMED) && countConfirmed >= limit) {
            repository.rejectPendingRequests(eventId, ParticipationStatus.PENDING);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    private Event getEventAndVerifyOwner(Long userId, Long eventId) {
        Event event = eventService.findEntityById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не найдено");
        }
        return event;
    }
}
