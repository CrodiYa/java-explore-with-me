package ru.practicum.ewm.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.exception.GlobalExceptionHandler;
import ru.practicum.ewm.model.compilation.CompilationDto;
import ru.practicum.ewm.model.compilation.NewCompilationDto;
import ru.practicum.ewm.model.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.model.event.EventShortDto;
import ru.practicum.ewm.service.compilation.CompilationService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminCompilationController.class)
@ContextConfiguration(classes = {AdminCompilationController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    private CompilationDto compilationDto;
    private NewCompilationDto newCompilationDto;
    private UpdateCompilationRequest updateRequest;

    @BeforeEach
    public void setUp() {
        EventShortDto eventShortDto = EventShortDto.builder()
                .id(1L)
                .title("Event 1")
                .build();

        compilationDto = CompilationDto.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(eventShortDto))
                .build();

        newCompilationDto = NewCompilationDto.builder()
                .title("New Compilation")
                .pinned(true)
                .events(Set.of(1L, 2L))
                .build();

        updateRequest = UpdateCompilationRequest.builder()
                .title("Updated Compilation")
                .pinned(false)
                .events(Set.of(3L))
                .build();
    }

    @Nested
    class AddingCompilation {

        @Test
        public void shouldAddCompilation() throws Exception {
            when(compilationService.addCompilation(any(NewCompilationDto.class)))
                    .thenReturn(compilationDto);

            mockMvc.perform(post("/admin/compilations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(newCompilationDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.title", is("Test Compilation")))
                    .andExpect(jsonPath("$.pinned", is(true)))
                    .andExpect(jsonPath("$.events", hasSize(1)));

            verify(compilationService).addCompilation(any(NewCompilationDto.class));
        }

        @Test
        public void shouldReturnBadRequestWhenAddingCompilationWithBlankTitle() throws Exception {
            NewCompilationDto invalidDto = NewCompilationDto.builder()
                    .title("")
                    .pinned(true)
                    .build();

            mockMvc.perform(post("/admin/compilations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());

            verify(compilationService, never()).addCompilation(any());
        }

        @Test
        public void shouldReturnBadRequestWhenAddingCompilationWithTitleTooLong() throws Exception {
            String longTitle = "a".repeat(51);
            NewCompilationDto invalidDto = NewCompilationDto.builder()
                    .title(longTitle)
                    .pinned(true)
                    .build();

            mockMvc.perform(post("/admin/compilations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());

            verify(compilationService, never()).addCompilation(any());
        }

        @Test
        public void shouldAddCompilationWithDefaultPinnedValue() throws Exception {
            NewCompilationDto dtoWithDefaultPinned = NewCompilationDto.builder()
                    .title("Default Pinned")
                    .events(Set.of(1L))
                    .build();

            CompilationDto resultDto = CompilationDto.builder()
                    .id(2L)
                    .title("Default Pinned")
                    .pinned(false)
                    .events(List.of())
                    .build();

            when(compilationService.addCompilation(any(NewCompilationDto.class)))
                    .thenReturn(resultDto);

            mockMvc.perform(post("/admin/compilations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoWithDefaultPinned)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pinned", is(false)));

            verify(compilationService).addCompilation(any(NewCompilationDto.class));
        }
    }

    @Nested
    class UpdatingCompilation {

        @Test
        public void shouldUpdateCompilation() throws Exception {
            when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                    .thenReturn(compilationDto);

            mockMvc.perform(patch("/admin/compilations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.title", is("Test Compilation")));

            verify(compilationService).updateCompilation(eq(1L), any(UpdateCompilationRequest.class));
        }

        @Test
        public void shouldUpdateCompilationWithOnlyTitle() throws Exception {
            UpdateCompilationRequest request = UpdateCompilationRequest.builder()
                    .title("Only Title")
                    .build();

            CompilationDto updatedDto = CompilationDto.builder()
                    .id(1L)
                    .title("Only Title")
                    .pinned(true)
                    .events(List.of())
                    .build();

            when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                    .thenReturn(updatedDto);

            mockMvc.perform(patch("/admin/compilations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Only Title")))
                    .andExpect(jsonPath("$.pinned", is(true)));

            verify(compilationService).updateCompilation(eq(1L), any(UpdateCompilationRequest.class));
        }

        @Test
        public void shouldReturnBadRequestWhenUpdatingWithTitleTooLong() throws Exception {
            String longTitle = "a".repeat(51);
            UpdateCompilationRequest invalidRequest = UpdateCompilationRequest.builder()
                    .title(longTitle)
                    .build();

            mockMvc.perform(patch("/admin/compilations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(compilationService, never()).updateCompilation(anyLong(), any());
        }

        @Test
        public void shouldReturnBadRequestWhenUpdatingWithNegativeCompilationId() throws Exception {
            mockMvc.perform(patch("/admin/compilations/-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest());

            verify(compilationService, never()).updateCompilation(anyLong(), any());
        }
    }

    @Nested
    class DeletingCompilation {

        @Test
        public void shouldDeleteCompilation() throws Exception {
            doNothing().when(compilationService).deleteCompilation(1L);

            mockMvc.perform(delete("/admin/compilations/1"))
                    .andExpect(status().isNoContent());

            verify(compilationService).deleteCompilation(1L);
        }

        @Test
        public void shouldReturnBadRequestWhenDeletingWithNegativeCompilationId() throws Exception {
            mockMvc.perform(delete("/admin/compilations/-1"))
                    .andExpect(status().isBadRequest());

            verify(compilationService, never()).deleteCompilation(anyLong());
        }

        @Test
        public void shouldReturnBadRequestWhenDeletingWithZeroCompilationId() throws Exception {
            mockMvc.perform(delete("/admin/compilations/0"))
                    .andExpect(status().isBadRequest());

            verify(compilationService, never()).deleteCompilation(anyLong());
        }
    }
}