package ru.practicum.ewm.controller.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.model.event.EventDtoRequest;
import ru.practicum.ewm.model.event.EventFullDto;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.service.event.EventService;
import ru.practicum.ewm.validation.OnUpdate;

import java.util.List;

import static ru.practicum.dto.Formatter.PATTERN;

@Slf4j
@RestController
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> findEvents(@RequestParam(required = false) List<Long> users,
                                         @RequestParam(required = false) List<EventState> states,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) @JsonFormat(pattern = PATTERN) String rangeStart,
                                         @RequestParam(required = false) @JsonFormat(pattern = PATTERN) String rangeEnd,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                         @RequestParam(defaultValue = "10") @Positive Integer size) {

        return eventService.findEvents(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto findEvents(@PathVariable @Positive Long eventId,
                                   @RequestBody @Validated(OnUpdate.class) EventDtoRequest request) {

        return eventService.patchAdminEvent(eventId, request);
    }

}
