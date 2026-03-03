package ru.practicum.ewm.controller.pub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.exception.GlobalExceptionHandler;
import ru.practicum.ewm.model.compilation.CompilationDto;
import ru.practicum.ewm.model.event.EventShortDto;
import ru.practicum.ewm.service.compilation.CompilationService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicCompilationController.class)
@ContextConfiguration(classes = {PublicCompilationController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    private CompilationDto compilationDto1;
    private CompilationDto compilationDto2;
    private List<CompilationDto> compilationDtos;

    @BeforeEach
    public void setUp() {
        EventShortDto eventShortDto = EventShortDto.builder()
                .id(1L)
                .title("Event 1")
                .build();

        compilationDto1 = CompilationDto.builder()
                .id(1L)
                .title("Compilation 1")
                .pinned(true)
                .events(List.of(eventShortDto))
                .build();

        compilationDto2 = CompilationDto.builder()
                .id(2L)
                .title("Compilation 2")
                .pinned(false)
                .events(List.of())
                .build();

        compilationDtos = List.of(compilationDto1, compilationDto2);
    }


    @Test
    public void shouldFindAllCompilationsWithoutPinnedFilter() throws Exception {
        when(compilationService.findCompilations(eq(null), eq(0), eq(10)))
                .thenReturn(compilationDtos);

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Compilation 1")))
                .andExpect(jsonPath("$[0].pinned", is(true)))
                .andExpect(jsonPath("$[0].events", hasSize(1)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].title", is("Compilation 2")))
                .andExpect(jsonPath("$[1].pinned", is(false)))
                .andExpect(jsonPath("$[1].events", hasSize(0)));

        verify(compilationService).findCompilations(null, 0, 10);
    }

    @Test
    public void shouldFindPinnedCompilations() throws Exception {
        List<CompilationDto> pinnedCompilations = List.of(compilationDto1);

        when(compilationService.findCompilations(eq(true), eq(0), eq(10)))
                .thenReturn(pinnedCompilations);

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pinned", is(true)));

        verify(compilationService).findCompilations(true, 0, 10);
    }

    @Test
    public void shouldFindNotPinnedCompilations() throws Exception {
        List<CompilationDto> notPinnedCompilations = List.of(compilationDto2);

        when(compilationService.findCompilations(eq(false), eq(0), eq(10)))
                .thenReturn(notPinnedCompilations);

        mockMvc.perform(get("/compilations")
                        .param("pinned", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pinned", is(false)));

        verify(compilationService).findCompilations(false, 0, 10);
    }

    @Test
    public void shouldFindCompilationsWithCustomPagination() throws Exception {
        when(compilationService.findCompilations(eq(null), eq(5), eq(5)))
                .thenReturn(compilationDtos);

        mockMvc.perform(get("/compilations")
                        .param("from", "5")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(compilationService).findCompilations(null, 5, 5);
    }

    @Test
    public void shouldUseDefaultPaginationWhenNotProvided() throws Exception {
        when(compilationService.findCompilations(eq(null), eq(0), eq(10)))
                .thenReturn(compilationDtos);

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk());

        verify(compilationService).findCompilations(null, 0, 10);
    }

    @Test
    public void shouldHandlePinnedParameterAsString() throws Exception {
        when(compilationService.findCompilations(eq(true), eq(0), eq(10)))
                .thenReturn(List.of(compilationDto1));

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true"))
                .andExpect(status().isOk());

        verify(compilationService).findCompilations(true, 0, 10);
    }

    @Test
    public void shouldReturnEmptyListWhenNoCompilations() throws Exception {
        when(compilationService.findCompilations(any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(compilationService).findCompilations(null, 0, 10);
    }

    @Test
    public void shouldFindCompilationById() throws Exception {
        when(compilationService.findCompilationById(1L))
                .thenReturn(compilationDto1);

        mockMvc.perform(get("/compilations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Compilation 1")))
                .andExpect(jsonPath("$.pinned", is(true)))
                .andExpect(jsonPath("$.events", hasSize(1)));

        verify(compilationService).findCompilationById(1L);
    }

    @Test
    public void shouldReturnBadRequestWhenFindingWithNegativeId() throws Exception {
        mockMvc.perform(get("/compilations/-1"))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).findCompilationById(anyLong());
    }

    @Test
    public void shouldReturnBadRequestWhenFindingWithZeroId() throws Exception {
        mockMvc.perform(get("/compilations/0"))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).findCompilationById(anyLong());
    }

    @Test
    public void shouldReturnBadRequestWhenFindingWithNonNumericId() throws Exception {
        mockMvc.perform(get("/compilations/abc"))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).findCompilationById(anyLong());
    }

    @Test
    public void shouldFindCompilationWithEmptyEvents() throws Exception {
        when(compilationService.findCompilationById(2L))
                .thenReturn(compilationDto2);

        mockMvc.perform(get("/compilations/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.title", is("Compilation 2")))
                .andExpect(jsonPath("$.pinned", is(false)))
                .andExpect(jsonPath("$.events", hasSize(0)));

        verify(compilationService).findCompilationById(2L);
    }
}
