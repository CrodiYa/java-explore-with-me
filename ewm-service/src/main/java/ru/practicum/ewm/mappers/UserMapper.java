package ru.practicum.ewm.mappers;

import ru.practicum.ewm.model.entity.user.User;
import ru.practicum.ewm.model.request.NewUserRequest;
import ru.practicum.ewm.model.response.UserDto;

public class UserMapper {

    public static User toEntity(NewUserRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
    }

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
