package ru.practicum.ewm.controller.pub;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.model.event.EventFullDto;
import ru.practicum.ewm.model.event.EventShortDto;
import ru.practicum.ewm.service.event.EventService;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> findPublicEvents(@RequestParam(required = false) String text,
                                                @RequestParam(required = false) List<Long> categories,
                                                @RequestParam(required = false) Boolean paid,
                                                @RequestParam(required = false) Instant rangeStart,
                                                @RequestParam(required = false) Instant rangeEnd,
                                                @RequestParam(defaultValue = "false") boolean onlyAvailable,
                                                @RequestParam(required = false) String sort,
                                                @RequestParam(defaultValue = "0") Integer from,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                HttpServletRequest request) {
        return eventService.findPublicEvents(text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, sort, from, size, request.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public EventFullDto findPublicEvent(@PathVariable(name = "id") Long eventId,
                                        HttpServletRequest request) {
        return eventService.findPublicEvent(eventId, request.getRemoteAddr());
    }
}
