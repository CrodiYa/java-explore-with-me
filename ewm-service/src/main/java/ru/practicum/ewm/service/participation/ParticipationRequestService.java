package ru.practicum.ewm.service.participation;

import ru.practicum.ewm.model.event.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.model.event.EventRequestStatusUpdateResult;
import ru.practicum.ewm.model.participation.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> findByRequesterId(Long requesterId);

    ParticipationRequestDto addParticipationRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId);

    // для PrivateEventController
    List<ParticipationRequestDto> findByEventId(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateStatusParticipationRequest(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request);
}
