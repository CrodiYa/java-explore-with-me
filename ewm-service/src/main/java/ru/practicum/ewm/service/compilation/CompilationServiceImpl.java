package ru.practicum.ewm.service.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.CompilationMapper;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.compilation.CompilationDto;
import ru.practicum.ewm.model.compilation.NewCompilationDto;
import ru.practicum.ewm.model.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public CompilationDto addCompilation(NewCompilationDto dto) {
        // достаём события по id (если переданы)
        Set<Event> events = getEventsFromIds(dto.getEvents());

        Compilation compilation = compilationMapper.toEntity(dto);
        compilation.setEvents(events);

        return compilationMapper.toDto(compilationRepository.save(compilation));
    }

    @Override
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest dto) {
        Compilation compilation = findEntityById(compilationId);

        // MapStruct обновит title и pinned (только если не null)
        compilationMapper.merge(compilation, dto);

        // events отдельно — потому что это Set<Long> - Set<Event>
        if (dto.getEvents() != null) {
            compilation.setEvents(getEventsFromIds(dto.getEvents()));
        }

        return compilationMapper.toDto(compilationRepository.save(compilation));
    }

    @Override
    public void deleteCompilation(Long compilationId) {
        if (!compilationRepository.existsById(compilationId)) {
            throw new NotFoundException("Подборка не найдена");
        }
        compilationRepository.deleteById(compilationId);
    }

    @Override
    public List<CompilationDto> findCompilations(Boolean pinned, Integer from, Integer size) {
        List<Compilation> compilations;
        PageRequest pageRequest = PageRequest.of(from / size, size);

        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageRequest);
        } else {
            compilations = compilationRepository.findAll(pageRequest).getContent();
        }

        return compilations.stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    @Override
    public CompilationDto findCompilationById(Long compilationId) {
        return compilationMapper.toDto(findEntityById(compilationId));
    }

    private Compilation findEntityById(Long compilationId) {
        return compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена"));
    }

    private Set<Event> getEventsFromIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(eventRepository.findAllById(eventIds));
    }
}
