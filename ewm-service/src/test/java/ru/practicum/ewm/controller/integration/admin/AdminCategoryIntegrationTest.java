package ru.practicum.ewm.controller.integration.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.RandomHelper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.category.CategoryDtoRequest;
import ru.practicum.ewm.repository.CategoryRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles(profiles = "test")
@Transactional
@AutoConfigureMockMvc
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class AdminCategoryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Nested
    class PostCategory {

        @Test
        public void shouldCreate() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest(RandomHelper.getRandomString());

            String responseContent = mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(request.getName()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode jsonNode = objectMapper.readTree(responseContent);
            Long returnedId = jsonNode.get("id").asLong();

            Optional<Category> category = categoryRepository.findById(returnedId);
            assertThat(category).isPresent();
            assertThat(category.get().getName()).isEqualTo(request.getName());
        }

        @Test
        public void shouldReturnConflictWhenConflict() throws Exception {
            String name = RandomHelper.getRandomString();
            categoryRepository.save(new Category(null, name));

            CategoryDtoRequest request = new CategoryDtoRequest(name);

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

            Category category = categoryRepository.save(new Category(null, RandomHelper.getRandomString()));
            categoryRepository.flush();
            String newName = RandomHelper.getRandomString();

            CategoryDtoRequest request = new CategoryDtoRequest(newName);

            mockMvc.perform(buildPatchRequest(category.getId(), request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(category.getId()))
                    .andExpect(jsonPath("$.name").value(newName));

            Optional<Category> patched = categoryRepository.findById(category.getId());

            assertThat(patched).isPresent();
            assertThat(patched.get().getName()).isEqualTo(request.getName());
        }

        @Test
        public void shouldReturnNotFoundWhenNotFound() throws Exception {
            CategoryDtoRequest request = new CategoryDtoRequest(RandomHelper.getRandomString());

            mockMvc.perform(buildPatchRequest("10002", request))
                    .andExpect(status().isNotFound());
        }

        @Test
        public void shouldReturnConflictWhenConflict() throws Exception {
            String name = RandomHelper.getRandomString();

            Long id = categoryRepository.save(new Category(null, RandomHelper.getRandomString())).getId();
            categoryRepository.save(new Category(null, name));

            mockMvc.perform(buildPatchRequest(id, new CategoryDtoRequest(name)))
                    .andExpect(status().isConflict());
        }

        private RequestBuilder buildPatchRequest(String id, CategoryDtoRequest request) throws JsonProcessingException {
            return patch("/admin/categories/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request));
        }

        private RequestBuilder buildPatchRequest(Long id, CategoryDtoRequest request) throws JsonProcessingException {
            return buildPatchRequest(id + "", request);
        }
    }
}
