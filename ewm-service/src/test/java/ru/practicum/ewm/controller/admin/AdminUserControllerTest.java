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
import ru.practicum.ewm.model.request.NewUserRequest;
import ru.practicum.ewm.model.response.UserDto;
import ru.practicum.ewm.service.user.UserService;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {AdminUserController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Nested
    class GetUser {
        @Test
        public void shouldGetAllNoList() throws Exception {

            when(userService.getUsers(null, 0, 10)).thenReturn(
                    List.of(new UserDto(1L, "name", "valid@email.ru"))
            );

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("name"))
                    .andExpect(jsonPath("$[0].email").value("valid@email.ru"));
        }

        @Test
        public void shouldGetAllWithList() throws Exception {

            when(userService.getUsers(List.of(1L, 2L), 0, 10)).thenReturn(
                    List.of(new UserDto(1L, "name1", "valid@email.ru"),
                            new UserDto(2L, "name2", "valid@email.ru")));

            mockMvc.perform(get("/admin/users?ids=1&ids=2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("name1"))
                    .andExpect(jsonPath("$[0].email").value("valid@email.ru"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].name").value("name2"))
                    .andExpect(jsonPath("$[1].email").value("valid@email.ru"));
        }

        @Test
        public void shouldReturnOkWhenFromIsZero() throws Exception {
            mockMvc.perform(get("/admin/users?from=0")).andExpect(status().isOk());
        }

        @Test
        public void shouldReturnBadRequestWhenFromInvalid() throws Exception {
            mockMvc.perform(get("/admin/users?from=a")).andExpect(status().isBadRequest());
            mockMvc.perform(get("/admin/users?from=-1")).andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnBadRequestWhenSizeInvalid() throws Exception {
            mockMvc.perform(get("/admin/users?size=a")).andExpect(status().isBadRequest());
            mockMvc.perform(get("/admin/users?size=-1")).andExpect(status().isBadRequest());
            mockMvc.perform(get("/admin/users?size=0")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    class PostUser {
        @Test
        public void shouldCreate() throws Exception {
            NewUserRequest request = new NewUserRequest("name", "valid@email.ru");
            when(userService.createUser(request))
                    .thenReturn(new UserDto(1L, "name", "valid@email.ru"));

            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("name"))
                    .andExpect(jsonPath("$.email").value("valid@email.ru"));
        }

        @Test
        public void shouldReturnBadRequestWhenRequestNameInvalid() throws Exception {
            NewUserRequest request = new NewUserRequest("", "valid@email.ru");

            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new NewUserRequest("     ", "valid@email.ru");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new NewUserRequest("a".repeat(251), "valid@email.ru");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnBadRequestWhenRequestEmailInvalid() throws Exception {
            NewUserRequest request = new NewUserRequest("name", "");

            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new NewUserRequest("name", "         ");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new NewUserRequest("name", "a@b.c");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new NewUserRequest("name", "a".repeat(255) + "@.email.ru");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new NewUserRequest("name", "12345678");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new NewUserRequest("name", "123456@");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());

            request = new NewUserRequest("name", "@@@@@@@@@@@@@a");
            mockMvc.perform(buildPostRequest(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        public void shouldReturnConflictWhenConflict() throws Exception {
            NewUserRequest request = new NewUserRequest("name", "valid@email.ru");

            when(userService.createUser(request))
                    .thenThrow(ConflictException.class);

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
            doThrow(NotFoundException.class).when(userService).deleteUser(1L);
            mockMvc.perform(buildDeleteRequest("1"))
                    .andExpect(status().isNotFound());
        }

        private RequestBuilder buildDeleteRequest(String id) throws JsonProcessingException {
            return delete("/admin/users/" + id);
        }
    }
}
