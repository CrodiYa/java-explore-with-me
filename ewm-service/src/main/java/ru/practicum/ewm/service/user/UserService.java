package ru.practicum.ewm.service.user;

import ru.practicum.ewm.model.entity.user.User;
import ru.practicum.ewm.model.request.NewUserRequest;
import ru.practicum.ewm.model.response.UserDto;

import java.util.List;

public interface UserService {

    User findEntityById(Long id);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto createUser(NewUserRequest request);

    void deleteUser(Long userId);

    void throwIfUserNotFound(Long userId);
}