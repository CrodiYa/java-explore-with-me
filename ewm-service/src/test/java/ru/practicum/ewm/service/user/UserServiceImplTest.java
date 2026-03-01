package ru.practicum.ewm.service.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.UserMapper;
import ru.practicum.ewm.model.request.NewUserRequest;
import ru.practicum.ewm.model.response.UserDto;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class UserServiceImplTest {
    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    private final NewUserRequest request = new NewUserRequest("name", "valid@email.ru");
    private final User user = new User(1L, "name", "valid@email.ru");
    private final UserDto userDto = new UserDto(1L, "name", "valid@email.ru");

    @Nested
    class SavingUser {
        @Test
        public void shouldSave() {
            when(repository.existsByEmail(any(String.class))).thenReturn(false);
            when(repository.save(any(User.class))).thenReturn(user);
            when(userMapper.toEntity(any(NewUserRequest.class))).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(userDto);
            UserDto saved = userService.createUser(request);
            assertEquals(request.getName(), saved.getName());
            assertEquals(request.getEmail(), saved.getEmail());
            verify(repository).existsByEmail(any(String.class));
            verify(repository).save(any(User.class));
            verify(userMapper).toDto(any(User.class));
            verify(userMapper).toEntity(any(NewUserRequest.class));

        }

        @Test
        public void shouldThrowConflict() {
            when(repository.existsByEmail(any(String.class))).thenReturn(true);
            assertThrows(ConflictException.class, () -> userService.createUser(request));
            verify(repository).existsByEmail(any(String.class));
            verify(repository, never()).save(any(User.class));
        }
    }

    @Nested
    class FindingUserById {

        @Test
        public void shouldFindEntity() {
            when(repository.findById(any(Long.class))).thenReturn(Optional.of(user));

            User found = userService.findEntityById(1L);
            assertEquals(1L, found.getId());
            assertEquals("name", found.getName());

            verify(repository).findById(any(Long.class));
        }

        @Test
        public void shouldThrowNotFoundWhenEntity() {
            when(repository.findById(any(Long.class))).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> userService.findEntityById(1L));
            verify(repository).findById(any(Long.class));
        }

        @Test
        public void shouldGetAllUsers() {
            int from = 0;
            int size = 10;
            int pageIndex = from / size;

            List<User> users = List.of(
                    new User(1L, "name1", "valid@email.ru"),
                    new User(2L, "name2", "valid@email.ru")
            );

            List<UserDto> expected = List.of(
                    new UserDto(1L, "name1", "valid@email.ru"),
                    new UserDto(2L, "name2", "valid@email.ru")
            );

            Page<User> page = new PageImpl<>(users);
            Pageable pageable = PageRequest.of(pageIndex, size);

            when(repository.findAll(pageable)).thenReturn(page);

            when(userMapper.toDto(users.get(0))).thenReturn(expected.get(0));
            when(userMapper.toDto(users.get(1))).thenReturn(expected.get(1));

            List<UserDto> actual = userService.getUsers(null, from, size);

            assertEquals(expected, actual);
            verify(repository).findAll(pageable);
            verify(repository, never()).findByIdIn(anyList(), any(PageRequest.class));
            verify(userMapper, times(2)).toDto(any(User.class));
        }

        @Test
        public void shouldGetAllUsersWhenEmpty() {
            int from = 0;
            int size = 10;
            int pageIndex = from / size;

            List<User> users = List.of(
                    new User(1L, "name1", "valid@email.ru"),
                    new User(2L, "name2", "valid@email.ru")
            );

            List<UserDto> expected = List.of(
                    new UserDto(1L, "name1", "valid@email.ru"),
                    new UserDto(2L, "name2", "valid@email.ru")
            );

            Page<User> page = new PageImpl<>(users);
            Pageable pageable = PageRequest.of(pageIndex, size);

            when(repository.findAll(pageable)).thenReturn(page);
            when(userMapper.toDto(users.get(0))).thenReturn(expected.get(0));
            when(userMapper.toDto(users.get(1))).thenReturn(expected.get(1));

            List<UserDto> actual = userService.getUsers(Collections.emptyList(), from, size);

            assertEquals(expected, actual);
            verify(repository).findAll(pageable);
            verify(repository, never()).findByIdIn(anyList(), any(PageRequest.class));
            verify(userMapper, times(2)).toDto(any(User.class));
        }


        @Test
        public void shouldGetAllUsersFromList() {
            int from = 0;
            int size = 10;
            int pageIndex = from / size;

            List<Long> ids = List.of(1L, 2L);

            List<User> users = List.of(
                    new User(1L, "name1", "valid@email.ru"),
                    new User(2L, "name2", "valid@email.ru")
            );

            List<UserDto> expected = List.of(
                    new UserDto(1L, "name1", "valid@email.ru"),
                    new UserDto(2L, "name2", "valid@email.ru")
            );

            Pageable pageable = PageRequest.of(pageIndex, size);

            when(repository.findByIdIn(ids, pageable)).thenReturn(users);
            when(userMapper.toDto(users.get(0))).thenReturn(expected.get(0));
            when(userMapper.toDto(users.get(1))).thenReturn(expected.get(1));

            List<UserDto> actual = userService.getUsers(ids, from, size);

            assertEquals(expected, actual);
            verify(repository).findByIdIn(ids, pageable);
            verify(repository, never()).findAll(pageable);
        }
    }

    @Nested
    class DeletingUser {

        @Test
        public void shouldDelete() {
            when(repository.existsById(1L)).thenReturn(true);
            assertDoesNotThrow(() -> userService.deleteUser(1L));
            verify(repository).existsById(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        public void shouldThrowNotFound() {
            when(repository.existsById(1L)).thenReturn(false);
            assertThrows(NotFoundException.class, () -> userService.deleteUser(1L));
            verify(repository).existsById(1L);
        }
    }

    @Nested
    class ExistsUser {

        @Test
        public void shouldThrowWhenNotFound() {
            when(repository.existsById(any(Long.class))).thenReturn(false);
            assertThrows(NotFoundException.class, () -> userService.throwIfUserNotFound(1L));
            verify(repository).existsById(1L);
        }

        @Test
        public void shouldDoNothingWhenExists() {
            when(repository.existsById(any(Long.class))).thenReturn(true);
            assertDoesNotThrow(() -> userService.throwIfUserNotFound(1L));
            verify(repository).existsById(1L);
        }
    }
}
