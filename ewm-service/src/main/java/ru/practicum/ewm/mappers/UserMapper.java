package ru.practicum.ewm.mappers;

import org.mapstruct.Mapper;
import ru.practicum.ewm.model.entity.user.User;
import ru.practicum.ewm.model.request.NewUserRequest;
import ru.practicum.ewm.model.response.UserDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(NewUserRequest request);

    UserDto toDto(User user);
}
