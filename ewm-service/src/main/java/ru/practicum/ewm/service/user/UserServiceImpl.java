package ru.practicum.ewm.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.UserMapper;
import ru.practicum.ewm.model.entity.user.User;
import ru.practicum.ewm.model.request.NewUserRequest;
import ru.practicum.ewm.model.response.UserDto;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Override
    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        List<User> users;
        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findByIdIn(ids, pageable);
        } else {
            users = userRepository.findAll(pageable).getContent();
        }

        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email " + request.getEmail() + " уже используется");
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        log.info("Создан пользователь: id={}, name={}", savedUser.getId(), savedUser.getName());

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        userRepository.deleteById(userId);
        log.info("Удалён пользователь: id={}", userId);
    }

    @Override
    public void throwIfUserNotFound(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id " + userId + " not found");
        }
    }
}