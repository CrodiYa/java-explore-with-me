package ru.practicum.ewm.controller.priv;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.model.participation.ParticipationRequestDto;
import ru.practicum.ewm.service.participation.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/users/{userId}/requests")
@RequiredArgsConstructor
public class PrivateParticipationRequestController {
    private final ParticipationRequestService service;

    @GetMapping
    public List<ParticipationRequestDto> findByRequesterId(@PathVariable @Positive Long userId) {
        return service.findByRequesterId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addParticipationRequest(@PathVariable @Positive Long userId,
                                                           @RequestParam @Positive Long eventId) {
        return service.addParticipationRequest(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelParticipationRequest(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long requestId) {
        return service.cancelParticipationRequest(userId, requestId);
    }
}
