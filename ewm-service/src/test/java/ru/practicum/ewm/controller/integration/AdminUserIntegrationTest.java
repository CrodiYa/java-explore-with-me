package ru.practicum.ewm.controller.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.model.user.NewUserRequest;
import ru.practicum.ewm.repository.UserRepository;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles(profiles = "test")
@Transactional
@AutoConfigureMockMvc
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class AdminUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Nested
    class GetUser {

        @Test
        public void shouldGetAllNoList() throws Exception {
            User user1 = userRepository.save(new User(null, RandomHelper.getRandomString(),
                    RandomHelper.getRandomEmail()));
            User user2 = userRepository.save(new User(null, RandomHelper.getRandomString(),
                    RandomHelper.getRandomEmail()));

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(user1.getId().intValue(),
                            user2.getId().intValue())))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder(user1.getName(), user2.getName())))
                    .andExpect(jsonPath("$[*].email", containsInAnyOrder(user1.getEmail(), user2.getEmail())));
        }

        @Test
        public void shouldGetAllWithList() throws Exception {
            User user1 = userRepository.save(new User(null, RandomHelper.getRandomString(),
                    RandomHelper.getRandomEmail()));
            User user2 = userRepository.save(new User(null, RandomHelper.getRandomString(),
                    RandomHelper.getRandomEmail()));
            userRepository.save(new User(null, RandomHelper.getRandomString(),
                    RandomHelper.getRandomEmail()));

            mockMvc.perform(get("/admin/users?ids=" + user1.getId() + "&ids=" + user2.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(user1.getId().intValue(),
                            user2.getId().intValue())))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder(user1.getName(), user2.getName())))
                    .andExpect(jsonPath("$[*].email", containsInAnyOrder(user1.getEmail(), user2.getEmail())));
        }
    }

    @Nested
    class PostUser {
        @Test
        public void shouldCreate() throws Exception {
            NewUserRequest request = new NewUserRequest(RandomHelper.getRandomString(), RandomHelper.getRandomEmail());

            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(request.getName()))
                    .andExpect(jsonPath("$.email").value(request.getEmail()));
        }


        @Test
        public void shouldReturnConflictWhenConflict() throws Exception {
            String email = RandomHelper.getRandomEmail();

            userRepository.save(new User(null, RandomHelper.getRandomString(), email));

            NewUserRequest request = new NewUserRequest(RandomHelper.getRandomString(), email);

            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isConflict());
        }

        private RequestBuilder buildPostRequest(NewUserRequest request) throws JsonProcessingException {
            return post("/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request));
        }
    }

    @Nested
    class DeleteUser {

        @Test
        public void shouldDelete() throws Exception {
            Long id = userRepository.save(new User(null, RandomHelper.getRandomString(),
                    RandomHelper.getRandomEmail())).getId();

            mockMvc.perform(buildDeleteRequest(id + ""))
                    .andExpect(status().isNoContent());

            assertTrue(userRepository.findById(id).isEmpty());
        }

        @Test
        public void shouldReturnNotFoundWhenNotFound() throws Exception {
            mockMvc.perform(buildDeleteRequest("10000"))
                    .andExpect(status().isNotFound());
        }

        private RequestBuilder buildDeleteRequest(String id) throws JsonProcessingException {
            return delete("/admin/users/" + id);
        }
    }
}
