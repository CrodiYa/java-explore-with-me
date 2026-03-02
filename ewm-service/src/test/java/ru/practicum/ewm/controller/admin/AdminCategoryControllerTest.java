package ru.practicum.ewm.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.GlobalExceptionHandler;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.category.CategoryDtoRequest;
import ru.practicum.ewm.model.category.CategoryDto;
import ru.practicum.ewm.service.category.CategoryService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {AdminCategoryController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Nested
    class PostCategory {
        @Test
        public void shouldCreate() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest("name");
            when(categoryService.addCategory(request))
                    .thenReturn(new CategoryDto(1L, "name"));

            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("name"));
        }

        @Test
        public void shouldReturnBadRequestWhenRequestInvalid() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest("");

            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new CategoryDtoRequest(" ");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new CategoryDtoRequest("a".repeat(51));
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnConflictWhenConflict() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest("name");
            when(categoryService.addCategory(request))
                    .thenThrow(ConflictException.class);

            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isConflict());
        }

        private RequestBuilder buildPostRequest(CategoryDtoRequest request) throws JsonProcessingException {
            return post("/admin/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request));
        }
    }

    @Nested
    class PatchCategory {
        @Test
        public void shouldPatch() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest("name");
            when(categoryService.patchCategory(1L, request))
                    .thenReturn(new CategoryDto(1L, "new name"));

            mockMvc.perform(buildPatchRequest("1", request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("new name"));
        }

        @Test
        public void shouldReturnBadRequestWhenRequestInvalid() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest("");

            mockMvc.perform(buildPatchRequest("1", request))
                    .andExpect(status().isBadRequest());

            request = new CategoryDtoRequest(" ");
            mockMvc.perform(buildPatchRequest("1", request))
                    .andExpect(status().isBadRequest());

            request = new CategoryDtoRequest("a".repeat(51));
            mockMvc.perform(buildPatchRequest("1", request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnBadRequestWhenIdInvalid() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest("valid");

            mockMvc.perform(buildPatchRequest("0", request))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(buildPatchRequest("a", request))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(buildPatchRequest("-1", request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnNotFoundWhenNotFound() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest("name");
            doThrow(NotFoundException.class).when(categoryService).patchCategory(1L, request);
            mockMvc.perform(buildPatchRequest("1", request))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnConflictWhenConflict() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest("name");
            when(categoryService.patchCategory(1L, request))
                    .thenThrow(ConflictException.class);

            mockMvc.perform(buildPatchRequest("1", request))
                    .andExpect(status().isConflict());
        }

        private RequestBuilder buildPatchRequest(String id, CategoryDtoRequest request) throws JsonProcessingException {
            return patch("/admin/categories/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request));
        }
    }

    @Nested
    class DeleteCategory {

        @Test
        public void shouldDelete() throws Exception {
            mockMvc.perform(buildDeleteRequest("1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        public void shouldReturnBadRequestWhenIdInvalid() throws Exception {

            mockMvc.perform(buildDeleteRequest("0"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(buildDeleteRequest("a"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(buildDeleteRequest("-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnNotFoundWhenNotFound() throws Exception {
            doThrow(NotFoundException.class).when(categoryService).deleteCategory(1L);
            mockMvc.perform(buildDeleteRequest("1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnConflictWhenConflict() throws Exception {
            doThrow(ConflictException.class).when(categoryService).deleteCategory(1L);

            mockMvc.perform(buildDeleteRequest("1"))
                    .andExpect(status().isConflict());
        }

        private RequestBuilder buildDeleteRequest(String id) throws JsonProcessingException {
            return delete("/admin/categories/" + id);
        }
    }
}
