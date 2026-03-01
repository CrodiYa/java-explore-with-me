package ru.practicum.ewm.controller.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.RandomHelper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.repository.CategoryRepository;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles(profiles = "test")
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class PublicCategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void shouldGetById() throws Exception {
        String name = RandomHelper.getRandomString();

        Long id = categoryRepository.save(new Category(null, name)).getId();

        mockMvc.perform(get("/categories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    public void shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/categories/10000")).andExpect(status().isNotFound());
    }

    @Test
    public void shouldFindAll() throws Exception {
        String name1 = RandomHelper.getRandomString();
        String name2 = RandomHelper.getRandomString();

        Long id1 = categoryRepository.save(new Category(null, name1)).getId();
        Long id2 = categoryRepository.save(new Category(null, name2)).getId();

        mockMvc.perform(get("/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(id1.intValue(), id2.intValue())))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder(name1, name2)));
    }
}
