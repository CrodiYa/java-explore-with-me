package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.entity.user.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    // когда ids указаны — вернуть конкретных пользователей с пагинацией
    List<User> findByIdIn(List<Long> ids, Pageable pageable);

    // проверка уникальности email
    boolean existsByEmail(String email);
}
