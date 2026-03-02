package ru.practicum.ewm.controller.priv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.model.participation.ParticipationRequestDto;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.service.participation.ParticipationRequestService;
import ru.practicum.ewm.validation.OnCreate;
import ru.practicum.ewm.validation.OnUpdate;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {

    private final EventService eventService;
    private final ParticipationRequestService prService;

    @GetMapping
    public List<EventShortDto> findEventsByUserId(@PathVariable @Positive Long userId,
                                                  @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                  @RequestParam(defaultValue = "10") @Positive Integer size) {
        return eventService.findEventsByUserId(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findEventById(@PathVariable @Positive Long userId,
                                      @PathVariable @Positive Long eventId) {
        return eventService.findEventById(userId, eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable @Positive Long userId,
                                 @RequestBody @Validated(OnCreate.class) EventDtoRequest request) {
        return eventService.addEvent(userId, request);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto patchEvent(@PathVariable @Positive Long userId,
                                   @PathVariable @Positive Long eventId,
                                   @RequestBody @Validated(OnUpdate.class) EventDtoRequest request) {
        return eventService.patchEvent(userId, eventId, request);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getRequests(@PathVariable @Positive Long userId,
                                                     @PathVariable @Positive Long eventId) {
        return prService.findByEventId(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult patchRequests(@PathVariable @Positive Long userId,
                                                        @PathVariable @Positive Long eventId,
                                                        @RequestBody @Valid EventRequestStatusUpdateRequest request) {
        return prService.updateStatusParticipationRequest(userId, eventId, request);
    }
}
