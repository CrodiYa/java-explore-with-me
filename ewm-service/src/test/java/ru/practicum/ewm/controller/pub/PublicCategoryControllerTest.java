package ru.practicum.ewm.controller.pub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.exception.GlobalExceptionHandler;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.response.CategoryDto;
import ru.practicum.ewm.service.category.CategoryService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {PublicCategoryController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    public void shouldGetById() throws Exception {

        when(categoryService.findById(1L)).thenReturn(new CategoryDto(1L, "name"));

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("name"));
    }

    @Test
    public void shouldReturnNotFound() throws Exception {
        when(categoryService.findById(1L)).thenThrow(NotFoundException.class);
        mockMvc.perform(get("/categories/1")).andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWhenIdInvalid() throws Exception {
        mockMvc.perform(get("/categories/0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/categories/a")).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFindAll() throws Exception {
        when(categoryService.findAll(0, 10)).thenReturn(
                List.of(new CategoryDto(1L, "name1"), new CategoryDto(2L, "name2")));
        mockMvc.perform(get("/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("name1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("name2"));
    }

    @Test
    public void shouldReturnOkWhenFromIsZero() throws Exception {
        mockMvc.perform(get("/categories?from=0")).andExpect(status().isOk());
    }

    @Test
    public void shouldReturnBadRequestWhenFromInvalid() throws Exception {
        mockMvc.perform(get("/categories?from=a")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/categories?from=-1")).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenSizeInvalid() throws Exception {
        mockMvc.perform(get("/categories?size=a")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/categories?size=-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/categories?size=0")).andExpect(status().isBadRequest());
    }
}
