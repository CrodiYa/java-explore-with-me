package ru.practicum.ewm.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.compilation.Compilation;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    // для GET /compilations?pinned=true с пагинацией
    List<Compilation> findByPinned(Boolean pinned, PageRequest pageRequest);
}
