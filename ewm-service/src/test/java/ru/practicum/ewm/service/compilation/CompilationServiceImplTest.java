package ru.practicum.ewm.service.compilation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.CompilationMapper;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.compilation.CompilationDto;
import ru.practicum.ewm.model.compilation.NewCompilationDto;
import ru.practicum.ewm.model.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventShortDto;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CompilationMapper compilationMapper;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private CompilationDto compilationDto;
    private NewCompilationDto newCompilationDto;
    private UpdateCompilationRequest updateRequest;
    private Event event1;
    private Event event2;
    private Set<Long> eventIds;

    @BeforeEach
    public void setUp() {
        event1 = new Event();
        event1.setId(1L);

        event2 = new Event();
        event2.setId(2L);

        eventIds = Set.of(1L, 2L);

        compilation = Compilation.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(new HashSet<>(Set.of(event1, event2)))
                .build();

        compilationDto = CompilationDto.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(new EventShortDto(), new EventShortDto()))
                .build();

        newCompilationDto = NewCompilationDto.builder()
                .title("New Compilation")
                .pinned(true)
                .events(eventIds)
                .build();

        updateRequest = UpdateCompilationRequest.builder()
                .title("Updated Compilation")
                .pinned(false)
                .events(eventIds)
                .build();
    }

    @Nested
    class AddingCompilation {

        @Test
        public void shouldAddCompilationWithEvents() {
            Set<Event> events = Set.of(event1, event2);

            when(eventRepository.findAllById(eventIds)).thenReturn(List.of(event1, event2));
            when(compilationMapper.toEntity(newCompilationDto)).thenReturn(compilation);
            when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            CompilationDto result = compilationService.addCompilation(newCompilationDto);

            assertNotNull(result);
            assertEquals(compilationDto.getId(), result.getId());
            assertEquals(compilationDto.getTitle(), result.getTitle());
            assertEquals(compilationDto.getPinned(), result.getPinned());

            verify(eventRepository).findAllById(eventIds);
            verify(compilationMapper).toEntity(newCompilationDto);
            verify(compilationRepository).save(compilation);
            verify(compilationMapper).toDto(compilation);
        }

        @Test
        public void shouldAddCompilationWithoutEvents() {
            NewCompilationDto dtoWithoutEvents = NewCompilationDto.builder()
                    .title("Empty Compilation")
                    .pinned(false)
                    .events(null)
                    .build();

            Compilation emptyCompilation = Compilation.builder()
                    .id(2L)
                    .title("Empty Compilation")
                    .pinned(false)
                    .events(new HashSet<>())
                    .build();

            CompilationDto emptyCompilationDto = CompilationDto.builder()
                    .id(2L)
                    .title("Empty Compilation")
                    .pinned(false)
                    .events(List.of())
                    .build();

            when(compilationMapper.toEntity(dtoWithoutEvents)).thenReturn(emptyCompilation);
            when(compilationRepository.save(emptyCompilation)).thenReturn(emptyCompilation);
            when(compilationMapper.toDto(emptyCompilation)).thenReturn(emptyCompilationDto);

            CompilationDto result = compilationService.addCompilation(dtoWithoutEvents);

            assertNotNull(result);
            assertEquals(emptyCompilationDto.getId(), result.getId());
            assertTrue(result.getEvents().isEmpty());

            verify(eventRepository, never()).findAllById(anySet());
            verify(compilationRepository).save(emptyCompilation);
        }

        @Test
        public void shouldAddCompilationWithEmptyEventSet() {
            NewCompilationDto dtoWithEmptyEvents = NewCompilationDto.builder()
                    .title("Empty Compilation")
                    .pinned(false)
                    .events(Set.of())
                    .build();

            Compilation emptyCompilation = Compilation.builder()
                    .id(2L)
                    .title("Empty Compilation")
                    .pinned(false)
                    .events(new HashSet<>())
                    .build();

            CompilationDto emptyCompilationDto = CompilationDto.builder()
                    .id(2L)
                    .title("Empty Compilation")
                    .pinned(false)
                    .events(List.of())
                    .build();

            when(compilationMapper.toEntity(dtoWithEmptyEvents)).thenReturn(emptyCompilation);
            when(compilationRepository.save(emptyCompilation)).thenReturn(emptyCompilation);
            when(compilationMapper.toDto(emptyCompilation)).thenReturn(emptyCompilationDto);

            CompilationDto result = compilationService.addCompilation(dtoWithEmptyEvents);

            assertNotNull(result);
            assertTrue(result.getEvents().isEmpty());
            verify(eventRepository, never()).findAllById(anySet());
        }
    }

    @Nested
    class UpdatingCompilation {

        @Test
        public void shouldUpdateAllFields() {
            when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
            when(eventRepository.findAllById(eventIds)).thenReturn(List.of(event1, event2));
            when(compilationRepository.save(compilation)).thenReturn(compilation);
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            CompilationDto result = compilationService.updateCompilation(1L, updateRequest);

            assertNotNull(result);
            verify(compilationMapper).merge(compilation, updateRequest);
            verify(compilationRepository).save(compilation);
            verify(eventRepository).findAllById(eventIds);
        }

        @Test
        public void shouldUpdateOnlyTitle() {
            UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                    .title("Only Title Update")
                    .build();

            when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
            when(compilationRepository.save(compilation)).thenReturn(compilation);
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            CompilationDto result = compilationService.updateCompilation(1L, request);

            assertNotNull(result);
            verify(compilationMapper).merge(compilation, request);
            verify(compilationRepository).save(compilation);
            verify(eventRepository, never()).findAllById(anySet());
        }

        @Test
        public void shouldUpdateOnlyPinned() {
            UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                    .pinned(false)
                    .build();

            when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
            when(compilationRepository.save(compilation)).thenReturn(compilation);
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            CompilationDto result = compilationService.updateCompilation(1L, request);

            assertNotNull(result);
            verify(compilationMapper).merge(compilation, request);
            verify(compilationRepository).save(compilation);
            verify(eventRepository, never()).findAllById(anySet());
        }

        @Test
        public void shouldUpdateOnlyEvents() {
            UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                    .events(Set.of(1L))
                    .build();

            when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
            when(eventRepository.findAllById(Set.of(1L))).thenReturn(List.of(event1));
            when(compilationRepository.save(compilation)).thenReturn(compilation);
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            CompilationDto result = compilationService.updateCompilation(1L, request);

            assertNotNull(result);
            verify(compilationMapper).merge(compilation, request);
            verify(compilationRepository).save(compilation);
            verify(eventRepository).findAllById(Set.of(1L));
        }

        @Test
        public void shouldThrowNotFoundExceptionWhenUpdatingNonExistingCompilation() {
            when(compilationRepository.findById(999L)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> compilationService.updateCompilation(999L, updateRequest));

            assertEquals("Подборка не найдена", exception.getMessage());
            verify(compilationRepository, never()).save(any());
        }
    }

    @Nested
    class DeletingCompilation {

        @Test
        public void shouldDeleteExistingCompilation() {
            when(compilationRepository.existsById(1L)).thenReturn(true);

            compilationService.deleteCompilation(1L);

            verify(compilationRepository).deleteById(1L);
        }

        @Test
        public void shouldThrowNotFoundExceptionWhenDeletingNonExistingCompilation() {
            when(compilationRepository.existsById(999L)).thenReturn(false);

            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> compilationService.deleteCompilation(999L));

            assertEquals("Подборка не найдена", exception.getMessage());
            verify(compilationRepository, never()).deleteById(any());
        }
    }

    @Nested
    class FindingCompilations {

        @Test
        public void shouldFindAllCompilationsWithoutPinnedFilter() {
            List<Compilation> compilations = List.of(compilation);
            PageRequest pageRequest = PageRequest.of(0, 10);

            when(compilationRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(compilations));
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            List<CompilationDto> result = compilationService.findCompilations(null, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(compilationRepository).findAll(pageRequest);
            verify(compilationRepository, never()).findByPinned(anyBoolean(), any());
        }

        @Test
        public void shouldFindPinnedCompilations() {
            List<Compilation> compilations = List.of(compilation);
            PageRequest pageRequest = PageRequest.of(0, 10);

            when(compilationRepository.findByPinned(true, pageRequest)).thenReturn(compilations);
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            List<CompilationDto> result = compilationService.findCompilations(true, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(compilationRepository).findByPinned(true, pageRequest);
            verify(compilationRepository, never()).findAll();
        }

        @Test
        public void shouldFindNotPinnedCompilations() {
            List<Compilation> compilations = List.of(compilation);
            PageRequest pageRequest = PageRequest.of(0, 10);

            when(compilationRepository.findByPinned(false, pageRequest)).thenReturn(compilations);
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            List<CompilationDto> result = compilationService.findCompilations(false, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(compilationRepository).findByPinned(false, pageRequest);
        }

        @Test
        public void shouldHandlePaginationCorrectly() {
            PageRequest expectedPageRequest = PageRequest.of(1, 5);

            when(compilationRepository.findAll(expectedPageRequest)).thenReturn(new PageImpl<>(List.of()));

            compilationService.findCompilations(null, 5, 5);

            verify(compilationRepository).findAll(PageRequest.of(1, 5));
        }
    }

    @Nested
    class FindingCompilationById {

        @Test
        public void shouldFindExistingCompilationById() {
            when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
            when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);

            CompilationDto result = compilationService.findCompilationById(1L);

            assertNotNull(result);
            assertEquals(compilationDto.getId(), result.getId());
        }

        @Test
        public void shouldThrowNotFoundExceptionWhenFindingNonExistingCompilation() {
            when(compilationRepository.findById(999L)).thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> compilationService.findCompilationById(999L));

            assertEquals("Подборка не найдена", exception.getMessage());
        }
    }
}